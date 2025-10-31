import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * RoleGuard - Role-based access control guard for AIRSEN
 *
 * This guard implements role-based authorization by checking if the current
 * user has the required role(s) to access a route. It works in conjunction
 * with AuthGuard to provide both authentication and authorization.
 *
 * How it works:
 * 1. Read required roles from route.data['roles']
 * 2. Get current user from AuthService
 * 3. Compare user role with required roles
 * 4. Allow access if role matches, redirect to /unauthorized if not
 *
 * Usage in routing:
 * {
 *   path: 'admin',
 *   loadChildren: () => import('./features/admin/admin.module').then(m => m.AdminModule),
 *   canActivate: [AuthGuard, RoleGuard],
 *   data: { roles: ['ADMIN'] }
 * }
 *
 * Multiple roles example:
 * {
 *   path: 'forum',
 *   component: ForumComponent,
 *   canActivate: [AuthGuard, RoleGuard],
 *   data: { roles: ['USER', 'ADMIN'] }
 * }
 *
 * Important:
 * - Always use AuthGuard BEFORE RoleGuard in canActivate array
 * - AuthGuard validates JWT token, RoleGuard checks role
 * - If no user is authenticated, redirects to /auth/login
 * - If user lacks required role, redirects to /unauthorized
 */
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /**
   * Determines if a route can be activated based on user role
   *
   * @param route - Activated route snapshot containing route data
   * @returns Observable<boolean> - true if user has required role, false otherwise
   */
  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    const requiredRoles = route.data['roles'] as string[];

    return this.authService.currentUser$.pipe(
      map(user => {
        // User not authenticated - redirect to login
        if (!user) {
          this.router.navigate(['/auth/login']);
          return false;
        }

        // No role requirement specified - allow access
        if (!requiredRoles || requiredRoles.length === 0) {
          return true;
        }

        // Check if user role matches any required role
        if (!requiredRoles.includes(user.role)) {
          console.warn(
            `RoleGuard: Access denied. User role '${user.role}' not in required roles: [${requiredRoles.join(', ')}]`
          );
          this.router.navigate(['/unauthorized']);
          return false;
        }

        // User has required role - allow access
        return true;
      })
    );
  }
}
