import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SidebarComponent } from './sidebar.component';
import { AuthService } from '@/auth/services/auth.service';
import { Router } from '@angular/router';

describe('SidebarComponent', () => {
  let component: SidebarComponent;
  let fixture: ComponentFixture<SidebarComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['getCurrentUser', 'logout', 'hasRole']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate'], { url: '/dashboard', events: jasmine.createSpy() });

    await TestBed.configureTestingModule({
      declarations: [SidebarComponent],
      imports: [RouterTestingModule, MatIconModule, MatTooltipModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

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

    expect(component.userName).toBe('User Name');
    expect(component.userEmail).toBe('');
    expect(component.userInitial).toBe('U');
  });

  it('should toggle sidebar state', () => {
    component.isExpanded = true;

    component.toggleSidebar();

    expect(component.isExpanded).toBe(false);
    expect(localStorage.getItem('sidebarExpanded')).toBe('false');
  });

  it('should load sidebar state from localStorage', () => {
    localStorage.setItem('sidebarExpanded', 'false');

    component.ngOnInit();

    expect(component.isExpanded).toBe(false);
  });

  it('should call logout and navigate on logout', () => {
    component.onLogout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
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

    const visibleItems = component.visibleNavItems;
    const adminItems = visibleItems.filter(item => item.label === 'Administration' || item.label === 'Alertes');

    expect(adminItems.length).toBeGreaterThan(0);
  });

  it('should check if route is active', () => {
    component.currentRoute = '/dashboard/overview';

    expect(component.isActive('/dashboard')).toBe(true);
    expect(component.isActive('/profile')).toBe(false);
  });
});
