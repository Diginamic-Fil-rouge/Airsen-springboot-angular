import { NgModule, APP_INITIALIZER } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { provideAnimations } from "@angular/platform-browser/animations";
import { provideHttpClient, withInterceptors } from "@angular/common/http";

// Core & Shared Modules
import { CoreModule } from "./core/core.module";
import { SharedModule } from "./shared/shared.module";

// App Components
import { AppComponent } from "./app.component";
import { AppRoutingModule } from "./app-routing.module";

// Layout Components (always loaded - required for app shell)
import { HeaderComponent } from "./layouts/components/header/header.component";
import { FooterComponent } from "./layouts/components/footer/footer.component";

// Error Page (always loaded - needed for wildcard route)
import { NotFoundComponent } from "./features/not-found/not-found.component";

// Services & Interceptors
import { authInterceptorFn } from "./core/interceptors/auth.interceptor";
import { CommuneDataService } from "./core/services/commune-data.service";
import { initializeCommunes } from "./core/initializers/commune-loader.initializer";

/**
 * AppModule - Root module for AIRSEN Angular application
 *
 * This module has been refactored to follow Angular best practices:
 * - CoreModule imported ONCE for singleton services (AuthService, AlertService, etc.)
 * - SharedModule imported for shared components, pipes, and Material modules
 * - All feature components lazy-loaded via AppRoutingModule
 * - Only layout components and NotFoundComponent declared here
 *
 * Components declared here:
 * - AppComponent: Root application component
 * - HeaderComponent: Top navigation bar (always visible)
 * - FooterComponent: Bottom footer (always visible)
 * - NotFoundComponent: 404 error page (needed for wildcard route)
 *
 * Note: SidebarComponent is now in SharedModule for better reusability
 *
 * All feature components are lazy-loaded:
 * - AuthModule: /auth (login, register)
 * - MapModule: /map (interactive air quality map)
 * - ForumModule: /forum (community discussions)
 * - DashboardModule: /dashboard (user dashboard) - protected
 * - HomeModule: / (landing page)
 * - ProfileModule: /profile (user profile) - protected
 *
 * Interceptors:
 * - authInterceptorFn: Adds JWT token to HTTP requests
 */
@NgModule({
  declarations: [
    AppComponent,
    // Layout Components (always loaded for app shell)
    HeaderComponent,
    FooterComponent,
    // Error Page (needed for wildcard route)
    NotFoundComponent,
    // NO FEATURE COMPONENTS - all lazy-loaded via routing
    // SidebarComponent moved to SharedModule for reusability
  ],
  imports: [
    BrowserModule,
    CoreModule, // Singleton services (import ONCE)
    SharedModule, // Shared components, pipes, Material modules
    AppRoutingModule, // Lazy-loaded feature modules
  ],
  providers: [
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptorFn])),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeCommunes,
      deps: [CommuneDataService],
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
