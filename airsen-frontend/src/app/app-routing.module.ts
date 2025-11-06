import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";

// Guards
import { AuthGuard } from "./core/auth/guards/auth.guard";
import { GuestGuard } from "./core/auth/guards/guest.guard";

// Components (only NotFoundComponent - not lazy loaded)
import { NotFoundComponent } from "./features/not-found/not-found.component";

/**
 * AppRoutingModule - Lazy-loaded routing configuration for AIRSEN
 *
 * This module implements lazy loading for all feature modules to optimize
 * initial bundle size and improve application performance.
 */
const routes: Routes = [
  // Default route - redirect to home
  {
    path: "",
    redirectTo: "/home",
    pathMatch: "full",
  },

  // Public Routes (no authentication required)
  {
    path: "home",
    loadChildren: () => import("./features/home/home.module").then((m) => m.HomeModule),
  },
  {
    path: "auth",
    loadChildren: () => import("./features/auth/auth.module").then((m) => m.AuthModule),
    canActivate: [GuestGuard], // Prevent authenticated users from login/register
  },

  // Map Route (public - anyone can view air quality data)
  {
    path: "map",
    loadChildren: () => import("./features/map/map.module").then((m) => m.MapModule),
  },
  // AQI Map Route (new full-screen air quality map interface)
  {
    path: 'aqi-map',
    loadChildren: () => import('./features/aqi-map/map.module').then(m => m.MapModule),
    canActivate: [AuthGuard]
  },

  // Protected Routes (require authentication with AuthGuard)
  {
    path: "dashboard",
    loadChildren: () => import("./features/dashboard/dashboard.module").then((m) => m.DashboardModule),
    canActivate: [AuthGuard],
  },
  {
    path: "profile",
    loadChildren: () => import("./features/profile/profile.module").then((m) => m.ProfileModule),
    canActivate: [AuthGuard],
  },
  {
    path: "favorites",
    loadChildren: () => import("./features/favorites/favorites.module").then((m) => m.FavoritesModule),
    canActivate: [AuthGuard],
  },
  {
    path: "forum",
    loadChildren: () => import("./features/forum/forum.module").then((m) => m.ForumModule),
    canActivate: [AuthGuard],
  },

  // Admin Routes (uncomment when AdminModule is created)
  // {
  //   path: 'admin',
  //   loadChildren: () => import('./features/admin/admin.module').then(m => m.AdminModule),
  //   canActivate: [AuthGuard, RoleGuard],
  //   data: { roles: ['ADMIN'] }
  // },

  // Error Handling Routes
  {
    path: "404",
    component: NotFoundComponent,
  },

  // Wildcard route - must be LAST to catch all unmapped URLs
  {
    path: "**",
    component: NotFoundComponent,
  },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      enableTracing: false, // Set to true for debugging routes
      scrollPositionRestoration: "top", // Scroll to top on route change
      anchorScrolling: "enabled", // Enable anchor scrolling (#section)
      onSameUrlNavigation: "reload", // Reload component on same URL navigation
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
