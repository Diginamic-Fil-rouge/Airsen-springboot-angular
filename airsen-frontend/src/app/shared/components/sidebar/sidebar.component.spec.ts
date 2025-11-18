import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BehaviorSubject, of } from 'rxjs';
import { SidebarComponent } from './sidebar.component';
import { AuthService } from '@/auth/services/auth.service';
import { Router } from '@angular/router';
import { SidebarService } from '@/shared/services/sidebar.service';

class SidebarServiceStub {
  private state$ = new BehaviorSubject<boolean>(true);
  sidebarExpanded$ = this.state$.asObservable();
  getSidebarState = jasmine.createSpy('getSidebarState').and.callFake(() => this.state$.value);
  toggleSidebar = jasmine.createSpy('toggleSidebar').and.callFake(() => {
    const newState = !this.state$.value;
    this.state$.next(newState);
    return newState;
  });

  emitState(value: boolean): void {
    this.state$.next(value);
  }
}

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;
  let routerNavigateSpy: jasmine.Spy;
  let sidebarService: SidebarServiceStub;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser', 'logout', 'hasRole']);
    sidebarService = new SidebarServiceStub();

    await TestBed.configureTestingModule({
      declarations: [SidebarComponent],
      imports: [RouterTestingModule, MatIconModule, MatTooltipModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: SidebarService, useValue: sidebarService }
      ]
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router);
    routerNavigateSpy = spyOn(router, 'navigate');

    fixture = TestBed.createComponent(SidebarComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with user data', () => {
    const mockUser = {
      id: 1,
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      role: 'USER'
    };
    authService.getCurrentUser.and.returnValue(mockUser);

    component.ngOnInit();

    expect(component.userName).toBe('John Doe');
    expect(component.userEmail).toBe('john@example.com');
    expect(component.userInitial).toBe('J');
  });

  it('should handle missing user data gracefully', () => {
    authService.getCurrentUser.and.returnValue(null);

    component.ngOnInit();

    expect(component.userName).toBe('');
    expect(component.userEmail).toBe('');
    expect(component.userInitial).toBe('');
  });

  it('should toggle sidebar state', () => {
    component.ngOnInit();
    component.isExpanded = true;

    component.toggleSidebar();

    expect(sidebarService.toggleSidebar).toHaveBeenCalled();
    expect(component.isExpanded).toBeFalse();
  });

  it('should load sidebar state from localStorage', () => {
    sidebarService.emitState(false);
    component.ngOnInit();

    expect(component.isExpanded).toBe(false);
  });

  it('should call logout and navigate on logout', () => {
    authService.logout.and.returnValue(of(void 0));
    component.ngOnInit();

    component.onLogout();

    expect(authService.logout).toHaveBeenCalled();
    expect(routerNavigateSpy).toHaveBeenCalledWith(['auth//login']);
  });

  it('should update backdrop visibility on window resize', () => {
    spyOnProperty(window, 'innerWidth', 'get').and.returnValue(800);

    component.isExpanded = true;
    component.onResize();

    expect(component.showBackdrop).toBe(true);
  });

  it('should not show backdrop on desktop', () => {
    spyOnProperty(window, 'innerWidth', 'get').and.returnValue(1200);

    component.isExpanded = true;
    component.onResize();

    expect(component.showBackdrop).toBe(false);
  });

  it('should hide backdrop when sidebar is collapsed', () => {
    spyOnProperty(window, 'innerWidth', 'get').and.returnValue(800);

    component.isExpanded = false;
    component.onResize();

    expect(component.showBackdrop).toBe(false);
  });

  it('should filter navigation items based on user roles', () => {
    authService.hasRole.and.callFake((role: string) => role === 'USER');

    const visibleItems = component.visibleNavItems;
    const adminItems = visibleItems.filter(item => item.roles?.includes('ADMIN'));

    expect(adminItems.length).toBe(0);
  });

  it('should show admin items for ADMIN role', () => {
    authService.hasRole.and.callFake((role: string) => role === 'ADMIN');

    expect(component.adminNavItems.length).toBeGreaterThan(0);
  });

  it('should check if route is active', () => {
    component.currentRoute = '/dashboard/overview';

    expect(component.isActive('/dashboard')).toBe(true);
    expect(component.isActive('/profile')).toBe(false);
  });
});
