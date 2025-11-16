import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// Components
import { AdminDashboardComponent } from './components/dashboard/admin-dashboard.component';
import { UserListComponent } from './components/users/user-list.component';
import { AlertListComponent } from './components/alerts/alert-list.component';
import { CampaignListComponent } from './components/campaigns/campaign-list.component';
import { AuditLogComponent } from './components/audit/audit-log.component';
import { CategoryListComponent } from './components/categories/category-list.component';

/**
 * Admin Routing Module
 *
 * Defines routes for the admin dashboard feature.
 * Routes are integrated with main app layout (no separate admin layout).
 */
const routes: Routes = [
  // Default route - dashboard
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  // Dashboard - statistics overview
  {
    path: 'dashboard',
    component: AdminDashboardComponent
  },
  // User Management
  {
    path: 'users',
    component: UserListComponent
  },
  // Alert Management
  {
    path: 'alerts',
    component: AlertListComponent
  },
  // Campaign Management
  {
    path: 'campaigns',
    component: CampaignListComponent
  },
  // Audit Log
  {
    path: 'audit',
    component: AuditLogComponent
  },
  // Category Management
  {
    path: 'categories',
    component: CategoryListComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
