import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";

// Auth Components
import { LoginComponent } from "./features/auth/login/login.component";
import { RegisterComponent } from "./features/auth/register/register.component";

// Feature Components
import { MapComponent } from "./features/map/map.component";
import { HomeComponent } from "./features/home/home.component";
import { NotFoundComponent } from "./features/not-found/not-found.component";
import { ForumComponent } from "./features/forum/forum.component";
import { ThreadDetailsComponent } from "./features/forum/threads/threads-details/thread-details.component";
import { AddThreadComponent } from "./features/forum/threads/add-thread/add-thread.component";
import { EditThreadComponent } from "./features/forum/threads/edit-thread/edit-thread.component";

// Guards
import { AuthGuard } from "./core/guards/auth.guard";

const routes: Routes = [
  // Default redirect to dashboard (authenticated landing page)
  { path: "", redirectTo: "/dashboard", pathMatch: "full" },

  // Public Routes (no authentication required)
  { path: "home", component: HomeComponent },

  // Auth Routes (login, register - no guards here)
  {
    path: "auth",
    children: [
      { path: "login", component: LoginComponent },
      { path: "register", component: RegisterComponent },
    ],
  },

  // Protected Routes (lazy loaded with AuthGuard)
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

  // Protected Feature Routes
  { path: "map", component: MapComponent, canActivate: [AuthGuard] },

  // Forum Routes
  { path: "forum", component: ForumComponent },
  { path: "forum/thread/:id", component: ThreadDetailsComponent },
  { path: "forum/add/thread", component: AddThreadComponent },
  { path: "forum/edit/thread/:id", component: EditThreadComponent },

  // Error Handling Routes
  { path: "404", component: NotFoundComponent },

  // Wildcard route - must be LAST to catch unmapped URLs
  { path: "**", component: NotFoundComponent },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      enableTracing: false, // Set to true for debugging
      scrollPositionRestoration: "top",
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
