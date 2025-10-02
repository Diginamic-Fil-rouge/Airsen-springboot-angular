import { Injectable, inject } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { AuthService } from '@/auth/services/auth.service';
import { UserRole } from '@/auth/models/user.model';

/**
 * AdminGuard - Protects admin-only routes
 *
 * Purpose: Prevent non-admin users from accessing administrative routes
 * Redirects: Non-admin authenticated users → /dashboard
 *           Unauthenticated users → /auth/login
 * Access: Visitors ❌ | Users ❌ | Admins ✅
 *
 * Usage:
 * {
 *   path: 'admin',
 *   component: AdminPanelComponent,
 *   canActivate: [AdminGuard]
 * }
 */
@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  private authService = inject(AuthService);
  private router = inject(Router);

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    return this.authService.currentUser$.pipe(
      take(1),
      map(user => {
        // Check if user is authenticated and has ADMIN role
        if (user && user.role === UserRole.ADMIN) {
          return true;
        }

        // If user is authenticated but not admin, redirect to dashboard
        if (user) {
          console.warn('Access denied: Admin privileges required');
          return this.router.createUrlTree(['/dashboard']);
        }

        // If user is not authenticated, redirect to login
        return this.router.createUrlTree(['/auth/login'], {
          queryParams: { returnUrl: state.url }
        });
      })
    );
  }
}
