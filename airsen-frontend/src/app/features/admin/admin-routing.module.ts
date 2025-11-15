import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// Components
import { AdminLayoutComponent } from './components/layout/admin-layout.component';
import { AdminDashboardComponent } from './components/dashboard/admin-dashboard.component';
import { UserListComponent } from './components/users/user-list.component';
import { AlertListComponent } from './components/alerts/alert-list.component';
import { CampaignListComponent } from './components/campaigns/campaign-list.component';
import { AuditLogComponent } from './components/audit/audit-log.component';

/**
 * Admin Routing Module
 *
 * Defines routes for the admin dashboard feature.
 * All routes use the AdminLayoutComponent wrapper for consistent navigation.
 */
const routes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
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
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
