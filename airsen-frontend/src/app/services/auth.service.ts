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
} from '../models/auth.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/auth`;
  private readonly TOKEN_KEY = 'airsen_access_token';
  private readonly REFRESH_TOKEN_KEY = 'airsen_refresh_token';
  private readonly USER_KEY = 'airsen_user';

  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);

  public currentUser$ = this.currentUserSubject.asObservable();
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadStoredAuth();
  }

  /**
   * Initialize authentication state from stored tokens
   */
  initializeAuthState(): Observable<boolean> {
    const token = this.getStoredToken();
    if (token && !this.isTokenExpired(token)) {
      const user = this.getStoredUser();
      if (user) {
        this.setCurrentUser(user);
        return new BehaviorSubject(true).asObservable();
      }
    }
    this.clearAuth();
    return new BehaviorSubject(false).asObservable();
  }

  /**
   * Login user
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials)
      .pipe(
        tap(response => {
          this.handleAuthSuccess(response);
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Register new user
   */
  register(userData: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/register`, userData)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Refresh access token
   */
  refreshToken(): Observable<RefreshTokenResponse> {
    const refreshToken = this.getStoredRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    const request: RefreshTokenRequest = { refreshToken };
    return this.http.post<RefreshTokenResponse>(`${this.apiUrl}/refresh`, request)
      .pipe(
        tap(response => {
          this.storeToken(response.accessToken);
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
    return this.getStoredToken();
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
    this.storeToken(response.accessToken);
    this.storeRefreshToken(response.refreshToken);
    this.storeUser(response.user);
    this.setCurrentUser(response.user);
  }

  private setCurrentUser(user: AuthUser): void {
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);
  }

  private clearAuth(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
  }

  private loadStoredAuth(): void {
    const user = this.getStoredUser();
    if (user && this.getStoredToken()) {
      this.setCurrentUser(user);
    }
  }

  private storeToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  private storeRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  private storeUser(user: AuthUser): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private getStoredRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  private getStoredUser(): AuthUser | null {
    const userStr = localStorage.getItem(this.USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = this.decodeJwt(token);
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }

  private decodeJwt(token: string): JwtPayload {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(c =>
      '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
    ).join(''));
    return JSON.parse(jsonPayload);
  }

  private handleError(error: any): Observable<never> {
    console.error('Auth service error:', error);
    return throwError(() => error);
  }
}