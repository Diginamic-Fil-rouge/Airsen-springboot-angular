import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// Auth Components
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';

// Feature Components
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { HomeComponent } from './features/home/home.component';
import { NotFoundComponent } from './features/not-found/not-found.component';
import { CarteComponent } from './features/carte/carte.component';
import { ForumComponent } from './features/forum/forum.component';
import { ThreadDetailsComponent } from './features/forum/threads/threads-details/thread-details.component';

// Guards - commented out until they are properly implemented
// import { AuthGuard } from './core/guards/auth.guard';
// import { GuestGuard } from './core/guards/guest.guard';

// TODO: Implement these components
// import { HomeComponent } from './components/pages/home/home.component';
// import { MapComponent } from './components/pages/map/map.component';
// import { ProfileComponent } from './components/pages/profile/profile.component';
// import { FavoritesComponent } from './components/pages/favorites/favorites.component';
// import { HistoryComponent } from './components/pages/history/history.component';
// import { ForumComponent } from './components/pages/forum/forum.component';
// import { NotificationsComponent } from './components/pages/notifications/notifications.component';

const routes: Routes = [
  // Public Routes
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },

  // Auth Routes (no guards for now to avoid compilation errors)
  {
    path: 'auth',
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent }
    ]
  },

  // Feature Routes (no guards for now to avoid compilation errors)
  { path: 'dashboard', component: DashboardComponent },
  { path: 'carte', component: CarteComponent },
  { path: 'forum', component: ForumComponent },
  { path: 'forum/thread/:id', component: ThreadDetailsComponent },

  // 404 Not Found Route
  { path: '404', component: NotFoundComponent },

  // Wildcard route - must be last
  { path: '**', component: NotFoundComponent },

  // TODO: Implement these components and uncomment
  // { path: 'map', component: MapComponent, canActivate: [AuthGuard] },
  // { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  // { path: 'favorites', component: FavoritesComponent, canActivate: [AuthGuard] },
  // { path: 'history', component: HistoryComponent, canActivate: [AuthGuard] },
  // { path: 'history/:commune', component: HistoryComponent, canActivate: [AuthGuard] },
  // { path: 'forum', component: ForumComponent, canActivate: [AuthGuard] },
  // { path: 'notifications', component: NotificationsComponent, canActivate: [AuthGuard] },
  
  // Lazy Loading for Forum Module (optional for future expansion)
  // {
  //   path: 'forum',
  //   loadChildren: () => import('./modules/forum/forum.module').then(m => m.ForumModule),
  //   canActivate: [AuthGuard]
  // }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    enableTracing: false, // Set to true for debugging
    scrollPositionRestoration: 'top'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule { }