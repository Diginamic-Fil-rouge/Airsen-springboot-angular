import { NgModule, Optional, SkipSelf } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

// Services
import { AuthService } from './auth/services/auth.service';
import { AlertService } from './services/alert.service';
import { ExportDataService } from './services/export-data.service';

// Guards
import { AuthGuard } from './auth/guards/auth.guard';
import { RoleGuard } from './auth/guards/role.guard';
import { GuestGuard } from './auth/guards/guest.guard';

/**
 * CoreModule - Singleton services and guards for AIRSEN application
 *
 * This module contains application-wide singleton services that should be
 * imported ONCE in AppModule. The self-injection check prevents accidental
 * re-imports in feature modules.
 *
 * Services:
 * - AuthService: JWT authentication and token management
 * - AlertService: Fetch and cache admin broadcast alerts
 * - ExportDataService: Client-side PDF/CSV generation with history (backend-aligned)
 *
 * Guards:
 * - AuthGuard: JWT token validation for protected routes
 * - RoleGuard: Role-based access control (ADMIN/USER)
 * - GuestGuard: Prevent authenticated users from accessing login/register
 */
@NgModule({
  imports: [
    CommonModule,
    HttpClientModule
  ],
  providers: [
    // Services
    AuthService,
    AlertService,
    ExportDataService,
    // Guards
    AuthGuard,
    RoleGuard,
    GuestGuard
  ]
})
export class CoreModule {
  /**
   * Self-injection check to prevent re-import of CoreModule
   *
   * Angular's dependency injection will throw an error if CoreModule
   * is imported in any module other than AppModule. This ensures
   * singleton behavior for all services in this module.
   *
   * @param parentModule - Injected CoreModule from parent injector (if exists)
   * @throws Error if CoreModule is already loaded in parent injector
   */
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    if (parentModule) {
      throw new Error(
        'CoreModule is already loaded. Import it in AppModule only.'
      );
    }
  }
}
