import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, BehaviorSubject } from 'rxjs';

import { FavoritesComponent } from './favorites.component';
import { FavoriteService } from '../../services/favorite.service';
import { AuthService } from '@/core/auth/services/auth.service';
import { UserFavoriteResponse, FavoriteCountResponse } from '@/shared/models/favorite.model';

describe('FavoritesComponent', () => {
  let component: FavoritesComponent;
  let fixture: ComponentFixture<FavoritesComponent>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockFavoriteService: any;
  let mockAuthService: any;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;

  const mockFavorites: UserFavoriteResponse[] = [
    {
      communeInseeCode: '75056',
      communeName: 'Paris',
      regionName: 'Île-de-France',
      departmentName: 'Paris',
      addedAt: '2025-11-14T10:00:00Z'
    },
    {
      communeInseeCode: '13055',
      communeName: 'Marseille',
      regionName: 'Provence-Alpes-Côte d\'Azur',
      departmentName: 'Bouches-du-Rhône',
      addedAt: '2025-11-14T09:00:00Z'
    }
  ];

  const mockFavoriteCount: FavoriteCountResponse = {
    count: 2,
    maximum: 10
  };

  beforeEach(async () => {
    // Create mock services with BehaviorSubjects for reactive state
    const favoritesSubject = new BehaviorSubject<UserFavoriteResponse[]>(mockFavorites);
    const countSubject = new BehaviorSubject<FavoriteCountResponse>(mockFavoriteCount);
    const loadingSubject = new BehaviorSubject<boolean>(false);
    const errorSubject = new BehaviorSubject<string | null>(null);

    mockFavoriteService = {
      favorites$: favoritesSubject.asObservable(),
      favoriteCount$: countSubject.asObservable(),
      loading$: loadingSubject.asObservable(),
      error$: errorSubject.asObservable(),
      getUserFavorites: jasmine.createSpy('getUserFavorites').and.returnValue(of(mockFavorites)),
      removeFavorite: jasmine.createSpy('removeFavorite').and.returnValue(of(void 0))
    };

    mockAuthService = {
      getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue({ id: 1, username: 'testuser' })
    };

    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [FavoritesComponent],
      providers: [
        { provide: FavoriteService, useValue: mockFavoriteService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
        { provide: MatDialog, useValue: mockDialog },
        { provide: MatSnackBar, useValue: mockSnackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FavoritesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('viewCommuneDetails', () => {
    it('should navigate to map with commune and openSidebar query params', () => {
      const communeInseeCode = '75056';

      component.viewCommuneDetails(communeInseeCode);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/map'],
        {
          queryParams: { commune: '75056', openSidebar: 'true' }
        }
      );
    });

    it('should navigate to map with correct commune for Marseille', () => {
      const communeInseeCode = '13055';

      component.viewCommuneDetails(communeInseeCode);

      expect(mockRouter.navigate).toHaveBeenCalledWith(
        ['/map'],
        {
          queryParams: { commune: '13055', openSidebar: 'true' }
        }
      );
    });

    it('should always include openSidebar=true in query params', () => {
      component.viewCommuneDetails('any-insee-code');

      const callArgs = mockRouter.navigate.calls.mostRecent().args;
      expect(callArgs[1]?.queryParams?.['openSidebar']).toBe('true');
    });
  });

  describe('initialization', () => {
    it('should load favorites on init for authenticated user', () => {
      expect(mockFavoriteService.getUserFavorites).toHaveBeenCalledWith(1);
    });

    it('should subscribe to favorites$', (done) => {
      mockFavoriteService.favorites$.subscribe((favorites: UserFavoriteResponse[]) => {
        expect(favorites).toEqual(mockFavorites);
        expect(component.state.favorites).toEqual(mockFavorites);
        done();
      });
    });

    it('should subscribe to favoriteCount$', (done) => {
      mockFavoriteService.favoriteCount$.subscribe((count: FavoriteCountResponse) => {
        expect(count).toEqual(mockFavoriteCount);
        expect(component.state.favoriteCount).toEqual(mockFavoriteCount);
        done();
      });
    });
  });

  describe('browseCommunes', () => {
    it('should navigate to communes list page', () => {
      component.browseCommunes();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/communes']);
    });
  });
});
