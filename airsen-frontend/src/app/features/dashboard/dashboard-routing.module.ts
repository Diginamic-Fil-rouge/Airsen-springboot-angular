import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { DashboardComponent } from "./dashboard.component";

/**
 * Dashboard Routes
 *
 * Purpose: Configure routing for dashboard feature module
 * Parent path: /dashboard (from AppRoutingModule)
 * Local paths: ''
 *
 * Routes:
 * - '' → DashboardComponent (default route when navigating to /dashboard)
 *
 * Features:
 * - Displays user's environmental data dashboard
 * - Shows favorite locations and their air quality/weather data
 * - Recent alerts and notifications
 * - Quick access to export functionality
 *
 * Security:
 * - Protected by AuthGuard at module level (in AppRoutingModule)
 * - Requires authenticated user (USER or ADMIN role minimum)
 *
 * Learning Point:
 * Feature modules should define their own routing with local paths only.
 * The parent path (/dashboard) is managed by the lazy loading configuration
 * in AppRoutingModule. This separation ensures modularity and reusability.
 */
const routes: Routes = [
  {
    path: "",
    component: DashboardComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DashboardRoutingModule {}
