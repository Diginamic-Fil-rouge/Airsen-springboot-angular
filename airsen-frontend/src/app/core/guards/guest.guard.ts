import { Injectable, inject } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { AuthService } from '@/auth/services/auth.service';
import { StorageService } from '@/auth/services/storage.service';

/**
 * GuestGuard - Protects guest-only routes (login/register)
 *
 * Purpose: Prevent authenticated users from accessing login/register pages
 * Redirects: Authenticated users → /dashboard or returnUrl
 * Access: Visitors  | Users  | Admins
 *
 * Usage:
 * {
 *   path: 'login',
 *   component: LoginComponent,
 *   canActivate: [GuestGuard]
 * }
 */
@Injectable({
  providedIn: 'root'
})
export class GuestGuard implements CanActivate {
  private authService = inject(AuthService);
  private router = inject(Router);
  private storageService = inject(StorageService);

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    return this.authService.isAuthenticated$.pipe(
      take(1),
      map(isAuthenticated => {
        if (!isAuthenticated) {
          // User is not authenticated, allow access to guest routes
          return true;
        }

        // User is authenticated, redirect to appropriate page
        const returnUrl = this.storageService.getReturnUrl();

        if (returnUrl && returnUrl !== state.url) {
          // Redirect to the stored return URL
          this.storageService.clearReturnUrl();
          return this.router.createUrlTree([returnUrl]);
        }

        // Default redirect to dashboard
        return this.router.createUrlTree(['/dashboard']);
      })
    );
  }
}
