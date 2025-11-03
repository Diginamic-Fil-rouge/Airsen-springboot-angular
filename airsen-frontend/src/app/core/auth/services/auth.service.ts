import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';

import { 
  LoginRequest, 
  LoginResponse, 
  RegisterRequest, 
  RegisterResponse, 
  RefreshTokenRequest,
  RefreshTokenResponse,
  AuthUser,
  JwtPayload 
} from '@/auth/models/auth.model';
import { environment } from '@/environments/environment';
import { TokenService } from '@/auth/services/token.service';
import { StorageService } from '@/auth/services/storage.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private isLoadingSubject = new BehaviorSubject<boolean>(false);
  private errorSubject = new BehaviorSubject<string | null>(null);

  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public isLoading$ = this.isLoadingSubject.asObservable();
  public error$ = this.errorSubject.asObservable();
  public userRole$ = this.currentUser$.pipe(map(user => user?.role || null));

  constructor(
    private http: HttpClient,
    private tokenService: TokenService,
    private storageService: StorageService
  ) {
    this.initializeAuth();
  }

  updateProfile(data: any) {
  const user = this.currentUserSubject.value;
  if (user) {
    const updatedUser = { ...user, ...data };
    this.currentUserSubject.next(updatedUser);
    this.storageService.storeUser(updatedUser);
    console.log('✅ Profil mis à jour localement :', updatedUser);
  }
}

  /**
   * Initialize authentication state from stored tokens
   */
  private initializeAuth(): void {
    if (this.tokenService.isTokenValid()) {
      const user = this.storageService.getStoredUser();
      if (user) {
        this.setCurrentUser(user);
        return;
      }
    }
    this.clearAuth();
  }

  /**
   * Check authentication state
   */
  checkAuthState(): Observable<boolean> {
    return this.isAuthenticated$;
  }

  /**
   * Login user
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    this.setLoading(true);
    this.clearError();
    
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap(response => {
          this.handleAuthSuccess(response);
          this.setLoading(false);
        }),
        catchError(error => {
          this.setLoading(false);
          return this.handleError(error);
        })
      );
  }

  /**
   * Register new user
   */
  register(userData: RegisterRequest): Observable<LoginResponse> {
    this.setLoading(true);
    this.clearError();

    return this.http.post<LoginResponse>(`${this.apiUrl}/register`, userData)
      .pipe(
        tap(response => {
          this.handleAuthSuccess(response);
          this.setLoading(false);
        }),
        catchError(error => {
          this.setLoading(false);
          return this.handleError(error);
        })
      );
  }

  /**
   * Refresh access token
   */
  refreshToken(): Observable<RefreshTokenResponse> {
    const refreshToken = this.tokenService.getRefreshToken();
    if (!refreshToken) {
      this.clearAuth();
      return throwError(() => new Error('No refresh token available'));
    }

    const request: RefreshTokenRequest = { refreshToken };
    return this.http.post<RefreshTokenResponse>(`${this.apiUrl}/refresh`, request)
      .pipe(
        tap(response => {
          this.tokenService.storeAccessToken(response.accessToken);
        }),
        catchError(error => {
          this.clearAuth();
          return this.handleError(error);
        })
      );
  }

  /**
   * Logout user
   */
  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {})
      .pipe(
        tap(() => {
          this.clearAuth();
        }),
        catchError(error => {
          // Clear auth even if logout request fails
          this.clearAuth();
          return this.handleError(error);
        })
      );
  }

  /**
   * Get current user
   */
  getCurrentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  /**
   * Get stored access token
   */
  getToken(): string | null {
    return this.tokenService.getAccessToken();
  }

  /**
   * Set return URL for redirect after login
   */
  setReturnUrl(url: string): void {
    this.storageService.storeReturnUrl(url);
  }

  /**
   * Get and clear return URL
   */
  getAndClearReturnUrl(): string {
    const url = this.storageService.getReturnUrl() || '/dashboard';
    this.storageService.clearReturnUrl();
    return url;
  }

  /**
   * Check if user has specific role
   */
  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user ? user.role === role : false;
  }

  /**
   * Check if user is admin
   */
  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  // Private methods

  private handleAuthSuccess(response: LoginResponse): void {
    this.tokenService.storeAccessToken(response.accessToken);
    this.tokenService.storeRefreshToken(response.refreshToken);

    // Map backend response to AuthUser
    const user: AuthUser = {
      id: response.userId,
      firstName: response.userFirstName,
      lastName: response.userLastName,
      email: response.userEmail,
      role: response.userRole
    };

    this.storageService.storeUser(user);
    this.setCurrentUser(user);
  }

  private setCurrentUser(user: AuthUser): void {
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);
    this.clearError();
  }

  private setLoading(loading: boolean): void {
    this.isLoadingSubject.next(loading);
  }

  private setError(error: string): void {
    this.errorSubject.next(error);
  }

  /**
   * Clear current error message
   * Useful for clearing errors when user starts typing or interacting with forms
   */
  clearError(): void {
    this.errorSubject.next(null);
  }

  private clearAuth(): void {
    this.tokenService.clearTokens();
    this.storageService.clearAll();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.setLoading(false);
    this.clearError();
  }


  private handleError(error: any): Observable<never> {
    const errorMessage = error?.error?.message || error?.message || 'Authentication error occurred';
    console.error('Auth service error:', error);
    this.setError(errorMessage);
    return throwError(() => error);
  }
}