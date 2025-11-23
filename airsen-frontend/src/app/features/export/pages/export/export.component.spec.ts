import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { ExportPageComponent } from './export.component';
import { FavoriteService } from '@/features/favorites/services/favorite.service';
import { ExportDataService } from '@/services/export-data.service';
import { AuthService } from '@/auth/services/auth.service';
import { UserFavoriteResponse, FavoriteCountResponse } from '@/shared/models/favorite.model';

describe('ExportPageComponent', () => {
  let component: ExportPageComponent;
  let fixture: ComponentFixture<ExportPageComponent>;
  let mockFavoriteService: jasmine.SpyObj<FavoriteService>;
  let mockExportDataService: jasmine.SpyObj<ExportDataService>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockCdr: jasmine.SpyObj<ChangeDetectorRef>;

  const mockUser = {
    id: 1,
    username: 'testuser',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    role: 'USER'
  };
  const mockFavorites: UserFavoriteResponse[] = [
    {
      communeInseeCode: '75056',
      communeName: 'Paris 16e Arrondissement',
      departmentName: 'Paris',
      regionName: 'Île-de-France',
      addedAt: '2024-01-01T00:00:00Z'
    }
  ];
  const mockCount: FavoriteCountResponse = { count: 1, maximum: 10 };

  beforeEach(async () => {
    // Create spies
    mockFavoriteService = jasmine.createSpyObj('FavoriteService', ['getUserFavorites'], {
      favorites$: new BehaviorSubject(mockFavorites),
      favoriteCount$: new BehaviorSubject(mockCount),
      loading$: new BehaviorSubject(false),
      error$: new BehaviorSubject(null)
    });
    mockExportDataService = jasmine.createSpyObj('ExportDataService', ['exportAsCSV']);
    mockAuthService = jasmine.createSpyObj('AuthService', ['getCurrentUser']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    mockCdr = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);

    await TestBed.configureTestingModule({
      declarations: [ExportPageComponent],
      providers: [
        { provide: FavoriteService, useValue: mockFavoriteService },
        { provide: ExportDataService, useValue: mockExportDataService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
        { provide: MatSnackBar, useValue: mockSnackBar },
        { provide: ChangeDetectorRef, useValue: mockCdr }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(ExportPageComponent);
    component = fixture.componentInstance;

    // Setup default mocks
    mockAuthService.getCurrentUser.and.returnValue(mockUser);
    mockFavoriteService.getUserFavorites.and.returnValue(of(mockFavorites));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize state properties', () => {
    expect(component.state.favorites).toEqual([]);
    expect(component.state.favoriteCount).toEqual({ count: 0, maximum: 10 });
    expect(component.state.isLoading).toBe(true);
    expect(component.state.error).toBe(null);
    expect(component.state.selectedCommune).toBe(null);
    expect(component.state.showCsvPanel).toBe(false);
  });

  it('should load favorites on init', () => {
    component.ngOnInit();

    expect(mockAuthService.getCurrentUser).toHaveBeenCalled();
    expect(mockFavoriteService.getUserFavorites).toHaveBeenCalledWith(1);
    expect(component.state.currentUserId).toBe(1);
  });

  it('should handle authentication error', () => {
    mockAuthService.getCurrentUser.and.returnValue(null);

    component.ngOnInit();

    expect(component.state.error).toBe('User not authenticated');
    expect(component.state.isLoading).toBe(false);
  });

  it('should show CSV panel when commune selected', () => {
    const commune = mockFavorites[0];

    component.onSelectCommuneForExport(commune);

    expect(component.state.selectedCommune).toBe(commune);
    expect(component.state.showCsvPanel).toBe(true);
    expect(mockCdr.markForCheck).toHaveBeenCalled();
  });

  it('should hide CSV panel on export complete', () => {
    component.state.showCsvPanel = true;
    component.state.selectedCommune = mockFavorites[0];

    component.onCsvExportComplete();

    expect(component.state.showCsvPanel).toBe(false);
    expect(component.state.selectedCommune).toBeNull();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'Export CSV completed successfully',
      'Close',
      jasmine.any(Object)
    );
  });

  it('should hide CSV panel on cancel', () => {
    component.state.showCsvPanel = true;
    component.state.selectedCommune = mockFavorites[0];

    component.onCsvExportCancelled();

    expect(component.state.showCsvPanel).toBe(false);
    expect(component.state.selectedCommune).toBeNull();
  });

  it('should navigate to map with commune details', () => {
    const inseeCode = '75056';

    component.viewCommuneDetails(inseeCode);

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/map'], {
      queryParams: { commune: inseeCode, openSidebar: 'true' }
    });
  });

  it('should navigate to map for browsing communes', () => {
    component.browseCommunes();

    expect(mockRouter.navigate).toHaveBeenCalledWith(['/map']);
  });

  it('should retry loading favorites', () => {
    component.state.error = 'Some error';

    component.retryLoad();

    expect(component.state.error).toBeNull();
    expect(mockFavoriteService.getUserFavorites).toHaveBeenCalled();
  });

  it('should handle favorites loading error', () => {
    mockFavoriteService.getUserFavorites.and.returnValue(throwError('API Error'));
    spyOn(console, 'error');

    component.ngOnInit();

    expect(console.error).toHaveBeenCalledWith('Failed to load favorites:', 'API Error');
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'Failed to load favorites. Please try again.',
      'Close',
      jasmine.any(Object)
    );
  });
});