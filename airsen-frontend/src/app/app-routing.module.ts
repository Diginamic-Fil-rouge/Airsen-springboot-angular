import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// Guards - TODO: Implement these guards
// import { AuthGuard } from './guards/auth.guard';
// import { GuestGuard } from './guards/guest.guard';

// Components - TODO: Implement these components
// import { HomeComponent } from './components/pages/home/home.component';
// import { LoginComponent } from './components/auth/login/login.component';
// import { RegisterComponent } from './components/auth/register/register.component';
// import { DashboardComponent } from './components/pages/dashboard/dashboard.component';
// import { MapComponent } from './components/pages/map/map.component';
// import { ProfileComponent } from './components/pages/profile/profile.component';
// import { FavoritesComponent } from './components/pages/favorites/favorites.component';
// import { HistoryComponent } from './components/pages/history/history.component';
// import { ForumComponent } from './components/pages/forum/forum.component';
// import { NotificationsComponent } from './components/pages/notifications/notifications.component';

const routes: Routes = [
  // TODO: Implement components and uncomment routes below
  
  // Temporary empty routes for initial compilation
  { path: '', redirectTo: '/', pathMatch: 'full' },
  
  // TODO: Uncomment and implement these routes when components are ready
  // Public Routes
  // { path: '', component: HomeComponent },
  // { path: 'login', component: LoginComponent, canActivate: [GuestGuard] },
  // { path: 'register', component: RegisterComponent, canActivate: [GuestGuard] },
  
  // Protected Routes (require authentication)
  // { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
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