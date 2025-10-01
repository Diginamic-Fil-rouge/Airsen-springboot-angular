import { Injectable, inject } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse,
  HttpHandlerFn
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap, finalize } from 'rxjs/operators';
import { AuthService } from '@/auth/auth.service';
import { TokenService } from '@/services/token.service';
import { Router } from '@angular/router';

/**
 * AuthInterceptor - Automatic JWT Token Management
 *
 * Responsibilities:
 * 1. Adds Authorization header with JWT token to all API requests
 * 2. Handles 401 Unauthorized errors with automatic token refresh
 * 3. Queues requests during token refresh to prevent duplicate refresh calls
 * 4. Prevents infinite loops by excluding auth endpoints from interception
 *
 * Flow:
 * - Outgoing Request → Add Bearer token if available
 * - 401 Response → Attempt token refresh
 * - Refresh Success → Retry original request with new token
 * - Refresh Failure → Logout user and redirect to login
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);
  private tokenService = inject(TokenService);
  private router = inject(Router);

  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip auth endpoints to prevent interceptor loops
    if (this.isAuthEndpoint(req.url)) {
      return next.handle(req);
    }

    // Add Authorization header if token exists
    const token = this.tokenService.getAccessToken();
    if (token && !this.tokenService.isTokenExpired(token)) {
      req = this.addTokenToRequest(req, token);
    }

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          return this.handle401Error(req, next);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Adds JWT token to request Authorization header
   */
  private addTokenToRequest(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  /**
   * Handles 401 Unauthorized errors with token refresh logic
   * Implements request queuing to prevent multiple refresh calls
   */
  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      const refreshToken = this.tokenService.getRefreshToken();

      if (!refreshToken) {
        // No refresh token available, logout user
        this.authService.logout();
        return throwError(() => new Error('Session expired. Please login again.'));
      }

      // Attempt token refresh
      return this.authService.refreshToken().pipe(
        switchMap((response: any) => {
          this.isRefreshing = false;
          const newAccessToken = response.accessToken;
          this.refreshTokenSubject.next(newAccessToken);

          // Retry the original request with new token
          return next.handle(this.addTokenToRequest(request, newAccessToken));
        }),
        catchError((error) => {
          this.isRefreshing = false;

          // Refresh failed, logout user
          this.authService.logout();
          this.router.navigate(['/auth/login'], {
            queryParams: { sessionExpired: 'true' }
          });

          return throwError(() => new Error('Session expired. Please login again.'));
        }),
        finalize(() => {
          this.isRefreshing = false;
        })
      );
    } else {
      // Token refresh is already in progress, queue this request
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => {
          return next.handle(this.addTokenToRequest(request, token!));
        })
      );
    }
  }

  /**
   * Checks if the request URL is an authentication endpoint
   * to prevent interceptor loops
   */
  private isAuthEndpoint(url: string): boolean {
    const authEndpoints = [
      '/api/v1/auth/login',
      '/api/v1/auth/register',
      '/api/v1/auth/refresh',
      '/api/v1/auth/logout'
    ];

    return authEndpoints.some(endpoint => url.includes(endpoint));
  }
}

/**
 * Functional Interceptor (Alternative Implementation for Standalone Components)
 *
 * Usage in app.config.ts:
 * providers: [
 *   provideHttpClient(withInterceptors([authInterceptorFn]))
 * ]
 */
export function authInterceptorFn(req: HttpRequest<any>, next: HttpHandlerFn): Observable<HttpEvent<any>> {
  const authService = inject(AuthService);
  const tokenService = inject(TokenService);
  const router = inject(Router);

  // Skip auth endpoints
  const authEndpoints = [
    '/api/v1/auth/login',
    '/api/v1/auth/register',
    '/api/v1/auth/refresh',
    '/api/v1/auth/logout'
  ];

  if (authEndpoints.some(endpoint => req.url.includes(endpoint))) {
    return next(req);
  }

  // Add token if available and not expired
  const token = tokenService.getAccessToken();
  if (token && !tokenService.isTokenExpired(token)) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        const refreshToken = tokenService.getRefreshToken();

        if (!refreshToken) {
          authService.logout();
          return throwError(() => new Error('Session expired. Please login again.'));
        }

        // Attempt token refresh
        return authService.refreshToken().pipe(
          switchMap((response: any) => {
            const newAccessToken = response.accessToken;

            // Retry original request with new token
            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newAccessToken}`
              }
            });

            return next(retryReq);
          }),
          catchError((refreshError) => {
            authService.logout();
            router.navigate(['/auth/login'], {
              queryParams: { sessionExpired: 'true' }
            });
            return throwError(() => new Error('Session expired. Please login again.'));
          })
        );
      }

      return throwError(() => error);
    })
  );
}
