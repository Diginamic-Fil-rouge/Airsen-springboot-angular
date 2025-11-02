import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError, of } from 'rxjs';
import { catchError, tap, map, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';

import {
  UserFavoriteResponse,
  AddFavoriteRequest,
  FavoriteCountResponse,
  FavoriteCheckResponse,
  FavoriteErrorResponse,
  FavoriteErrorCode
} from '@/shared/models/favorite.model';
import { AuthService } from '@/core/auth/services/auth.service';
import { environment } from '@/environments/environment';

/**
 *  Manages user favorites with reactive state management
 *
 * Architecture Pattern: Mirrors backend UserFavoritesService.java
 * - Service Layer: Business logic and state management
 * - Reactive Pattern: BehaviorSubject for state management (like backend JPA entities)
 * - Error Handling: Matches backend exception handling
 * - Authentication: Integrates with AuthService for JWT and user management
 */
@Injectable({
  providedIn: 'root'
})
export class FavoriteService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/users`;

  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private router = inject(Router);

  // Reactive state management (like backend JPA entities)
  private favoritesSubject = new BehaviorSubject<UserFavoriteResponse[]>([]);
  public favorites$ = this.favoritesSubject.asObservable();

  private favoriteCountSubject = new BehaviorSubject<FavoriteCountResponse>({ count: 0, maximum: 10 });
  public favoriteCount$ = this.favoriteCountSubject.asObservable();

  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  private errorSubject = new BehaviorSubject<string | null>(null);
  public error$ = this.errorSubject.asObservable();

  constructor() {
    this.initializeFavorites();
  }

  /**
   * Initialize favorites for authenticated user
   */
  private initializeFavorites(): void {
    this.authService.currentUser$.subscribe(user => {
      if (user) {
        this.loadUserFavorites(user.id);
        this.loadFavoriteCount(user.id);
      } else {
        this.clearState();
      }
    });
  }

  /**
   * Add commune to favorites
   * POST /api/v1/users/{userId}/favorites
   */
  addFavorite(userId: number, request: AddFavoriteRequest): Observable<UserFavoriteResponse> {
    this.setLoading(true);
    this.clearError();

    return this.http.post<UserFavoriteResponse>(`${this.apiUrl}/${userId}/favorites`, request)
      .pipe(
        tap(response => {
          // Optimistic update: add to local state
          const currentFavorites = this.favoritesSubject.value;
          this.favoritesSubject.next([...currentFavorites, response]);

          // Update count
          const currentCount = this.favoriteCountSubject.value;
          this.favoriteCountSubject.next({
            count: currentCount.count + 1,
            maximum: currentCount.maximum
          });

          this.setLoading(false);
        }),
        catchError(error => {
          this.setLoading(false);
          return this.handleError(error);
        })
      );
  }

  /**
   * Get all user favorites
   * GET /api/v1/users/{userId}/favorites
   */
  getUserFavorites(userId: number): Observable<UserFavoriteResponse[]> {
    this.setLoading(true);
    this.clearError();

    return this.http.get<UserFavoriteResponse[]>(`${this.apiUrl}/${userId}/favorites`)
      .pipe(
        tap(favorites => {
          this.favoritesSubject.next(favorites);
          this.setLoading(false);
        }),
        catchError(error => {
          this.setLoading(false);
          return this.handleError(error);
        })
      );
  }

  /**
   * Get favorites count (0-10)
   * GET /api/v1/users/{userId}/favorites/count
   */
  getFavoriteCount(userId: number): Observable<FavoriteCountResponse> {
    this.setLoading(true);
    this.clearError();

    return this.http.get<FavoriteCountResponse>(`${this.apiUrl}/${userId}/favorites/count`)
      .pipe(
        tap(count => {
          this.favoriteCountSubject.next(count);
          this.setLoading(false);
        }),
        catchError(error => {
          this.setLoading(false);
          return this.handleError(error);
        })
      );
  }

  /**
   * Remove favorite
   * DELETE /api/v1/users/{userId}/favorites/{communeInseeCode}
   */
  removeFavorite(userId: number, communeInseeCode: string): Observable<void> {
    this.setLoading(true);
    this.clearError();

    return this.http.delete<void>(`${this.apiUrl}/${userId}/favorites/${communeInseeCode}`)
      .pipe(
        tap(() => {
          // Optimistic update: remove from local state
          const currentFavorites = this.favoritesSubject.value;
          this.favoritesSubject.next(
            currentFavorites.filter(fav => fav.communeInseeCode !== communeInseeCode)
          );

          // Update count
          const currentCount = this.favoriteCountSubject.value;
          this.favoriteCountSubject.next({
            count: Math.max(0, currentCount.count - 1),
            maximum: currentCount.maximum
          });

          this.setLoading(false);
        }),
        catchError(error => {
          this.setLoading(false);
          return this.handleError(error);
        })
      );
  }

  /**
   * Check if commune is favorited
   * GET /api/v1/users/{userId}/favorites/{communeInseeCode}/check
   */
  checkFavorite(userId: number, communeInseeCode: string): Observable<FavoriteCheckResponse> {
    this.setLoading(true);
    this.clearError();

    return this.http.get<FavoriteCheckResponse>(`${this.apiUrl}/${userId}/favorites/${communeInseeCode}/check`)
      .pipe(
        tap(() => {
          this.setLoading(false);
        }),
        catchError(error => {
          this.setLoading(false);
          return this.handleError(error);
        })
      );
  }

  /**
   * Helper method for UI toggle functionality
   * Toggles favorite status: checks if exists, then adds or removes
   */
  toggleFavorite(userId: number, communeInseeCode: string): Observable<boolean> {
    return this.checkFavorite(userId, communeInseeCode)
      .pipe(
        switchMap(checkResponse => {
          if (checkResponse.isFavorited) {
            // Remove favorite
            return this.removeFavorite(userId, communeInseeCode)
              .pipe(map(() => false));
          } else {
            // Add favorite
            const request: AddFavoriteRequest = { communeInseeCode };
            return this.addFavorite(userId, request)
              .pipe(map(() => true));
          }
        }),
        catchError(error => {
          return this.handleError(error).pipe(map(() => false));
        })
      );
  }

  /**
   * Load user favorites (internal method)
   */
  private loadUserFavorites(userId: number): void {
    if (!userId) return;

    this.getUserFavorites(userId).subscribe({
      error: (error) => {
        console.error('Failed to load favorites:', error);
      }
    });
  }

  /**
   * Load favorite count (internal method)
   */
  private loadFavoriteCount(userId: number): void {
    if (!userId) return;

    this.getFavoriteCount(userId).subscribe({
      error: (error) => {
        console.error('Failed to load favorite count:', error);
      }
    });
  }

  /**
   * Refresh favorites and count for current user
   */
  refreshFavorites(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.loadUserFavorites(user.id);
      this.loadFavoriteCount(user.id);
    }
  }

  /**
   * Get current favorites
   */
  getCurrentFavorites(): UserFavoriteResponse[] {
    return this.favoritesSubject.value;
  }

  /**
   * Get current favorite count
   */
  getCurrentCount(): FavoriteCountResponse {
    return this.favoriteCountSubject.value;
  }

  /**
   * Check if specific commune is in current favorites
   */
  isInFavorites(communeInseeCode: string): boolean {
    return this.favoritesSubject.value.some(
      fav => fav.communeInseeCode === communeInseeCode
    );
  }

  /**
   * Check if user can add more favorites (under limit)
   */
  canAddMoreFavorites(): boolean {
    const count = this.favoriteCountSubject.value;
    return count.count < count.maximum;
  }

  /**
   * Clear all state (used on logout)
   */
  private clearState(): void {
    this.favoritesSubject.next([]);
    this.favoriteCountSubject.next({ count: 0, maximum: 10 });
    this.clearError();
  }

  // Private helper methods

  private setLoading(loading: boolean): void {
    this.loadingSubject.next(loading);
  }

  private setError(error: string): void {
    this.errorSubject.next(error);
  }

  private clearError(): void {
    this.errorSubject.next(null);
  }

  /**
   * Error handling matching backend exception patterns
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unexpected error occurred';

    if (error.error && typeof error.error === 'object') {
      const errorResponse = error.error as FavoriteErrorResponse;
      errorMessage = errorResponse.message || errorMessage;

      // Handle specific error codes
      switch (errorResponse.code as FavoriteErrorCode) {
        case FavoriteErrorCode.MAX_FAVORITES_REACHED:
          errorMessage = 'Maximum of 10 favorites reached. Please remove a favorite before adding a new one.';
          break;
        case FavoriteErrorCode.INVALID_INSEE_CODE:
          errorMessage = 'Invalid INSEE code. Must be exactly 5 digits.';
          break;
        case FavoriteErrorCode.COMMUNE_NOT_FOUND:
          errorMessage = 'Commune not found with the provided INSEE code.';
          break;
        case FavoriteErrorCode.FAVORITE_NOT_FOUND:
          errorMessage = 'Favorite not found.';
          break;
        case FavoriteErrorCode.ALREADY_FAVORITED:
          errorMessage = 'This commune is already in your favorites.';
          break;
        case FavoriteErrorCode.UNAUTHORIZED_ACCESS:
          errorMessage = 'Please login to manage your favorites.';
          this.router.navigate(['/auth/login']);
          break;
        case FavoriteErrorCode.FORBIDDEN_ACCESS:
          errorMessage = 'You do not have permission to access these favorites.';
          break;
      }
    } else {
      // Handle HTTP status codes
      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Bad request. Please check your input.';
          break;
        case 401:
          errorMessage = 'Session expired. Please login again.';
          this.router.navigate(['/auth/login'], {
            queryParams: { sessionExpired: 'true' }
          });
          break;
        case 403:
          errorMessage = 'You do not have permission to perform this action.';
          break;
        case 404:
          errorMessage = 'Resource not found.';
          break;
        case 409:
          errorMessage = 'Conflict: This commune is already in your favorites.';
          break;
        case 500:
          errorMessage = 'Server error. Please try again later.';
          break;
      }
    }

    this.setError(errorMessage);
    return throwError(() => new Error(errorMessage));
  }
}
