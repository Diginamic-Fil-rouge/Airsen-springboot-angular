import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { FavoriteButtonComponent } from './favorite-button.component';
import { FavoriteService } from '@/features/favorites/services/favorite.service';
import { AuthService } from '@/core/auth/services/auth.service';
import { FavoriteCountResponse, FavoriteCheckResponse } from '@/shared/models/favorite.model';

class AuthServiceStub {
  private currentUserSubject = new BehaviorSubject<{ id: number } | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  emitUser(user: { id: number } | null): void {
    this.currentUserSubject.next(user);
  }
}

class FavoriteServiceStub {
  private favoriteCountSubject = new BehaviorSubject<FavoriteCountResponse>({ count: 0, maximum: 10 });
  favoriteCount$ = this.favoriteCountSubject.asObservable();
  toggleFavorite = jasmine.createSpy('toggleFavorite').and.returnValue(of(true));
  checkFavorite = jasmine.createSpy('checkFavorite').and.returnValue(of({ isFavorited: false } as FavoriteCheckResponse));
  getCurrentCount = jasmine.createSpy('getCurrentCount').and.callFake(() => this.favoriteCountSubject.value);

  emitFavoriteCount(count: FavoriteCountResponse): void {
    this.favoriteCountSubject.next(count);
  }
}

describe('FavoriteButtonComponent', () => {
  let component: FavoriteButtonComponent;
  let fixture: ComponentFixture<FavoriteButtonComponent>;
  let favoriteService: FavoriteServiceStub;
  let authService: AuthServiceStub;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    favoriteService = new FavoriteServiceStub();
    authService = new AuthServiceStub();
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [FavoriteButtonComponent],
      providers: [
        { provide: FavoriteService, useValue: favoriteService },
        { provide: AuthService, useValue: authService },
        { provide: MatSnackBar, useValue: snackBar }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(FavoriteButtonComponent);
    component = fixture.componentInstance;
    component.communeInseeCode = '75056';
    fixture.detectChanges();
  });

  afterEach(() => {
    snackBar.open.calls.reset();
    favoriteService.toggleFavorite.calls.reset();
    favoriteService.checkFavorite.calls.reset();
  });

  const emitUser = (id = 1) => authService.emitUser({ id });

  it('should toggle favorite and emit event on success', fakeAsync(() => {
    emitUser();

    favoriteService.toggleFavorite.and.returnValue(of(true));
    favoriteService.getCurrentCount.and.returnValue({ count: 3, maximum: 10 });

    let emitted: boolean | undefined;
    component.favoriteToggled.subscribe(value => emitted = value);

    component.toggleFavorite();
    tick(300);

    expect(favoriteService.toggleFavorite).toHaveBeenCalledWith(1, '75056');
    expect(component.isFavorited).toBeTrue();
    expect(component.isLoading).toBeFalse();
    expect(emitted).toBeTrue();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Ajouté aux favoris (3/10)',
      'Fermer',
      jasmine.objectContaining({ duration: 3500 })
    );
  }));

  it('should show login message when no user is present', () => {
    component.toggleFavorite();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Veuillez vous connecter pour gérer vos favoris.',
      'Fermer',
      jasmine.objectContaining({ duration: 5000 })
    );
    expect(favoriteService.toggleFavorite).not.toHaveBeenCalled();
  });

  it('should prevent adding when maximum favorites reached', () => {
    emitUser();
    favoriteService.emitFavoriteCount({ count: 10, maximum: 10 });
    component.isFavorited = false;

    component.toggleFavorite();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Limite atteinte : 10 favoris maximum',
      'Fermer',
      jasmine.objectContaining({ duration: 5000 })
    );
    expect(favoriteService.toggleFavorite).not.toHaveBeenCalled();
  });

  it('should rollback state and show error when toggle fails', () => {
    emitUser();
    favoriteService.toggleFavorite.and.returnValue(throwError(() => new Error('maximum favorites limit reached')));

    component.toggleFavorite();

    expect(component.isFavorited).toBeFalse();
    expect(component.isLoading).toBeFalse();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Limite atteinte : 10 favoris maximum',
      'Fermer',
      jasmine.objectContaining({ duration: 5000 })
    );
  });
});
