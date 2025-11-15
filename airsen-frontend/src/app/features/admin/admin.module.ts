import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

// Shared Module
import { SharedModule } from '@/shared/shared.module';

// Routing
import { AdminRoutingModule } from './admin-routing.module';

// Components
import { AdminDashboardComponent } from './components/dashboard/admin-dashboard.component';
import { UserListComponent } from './components/users/user-list.component';
import { AlertListComponent } from './components/alerts/alert-list.component';
import { CampaignListComponent } from './components/campaigns/campaign-list.component';
import { AuditLogComponent } from './components/audit/audit-log.component';

/**
 * AdminModule - Admin dashboard feature module
 *
 * This module provides administrative functionality for AIRSEN including:
 * - User management (view, suspend/activate, role management)
 * - Alert signal management
 * - Notification campaign management
 * - Audit log viewing
 * - System statistics dashboard
 *
 * Access: Admin role only (enforced by RoleGuard)
 */
@NgModule({
  declarations: [
    AdminDashboardComponent,
    UserListComponent,
    AlertListComponent,
    CampaignListComponent,
    AuditLogComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    HttpClientModule,
    SharedModule,
    AdminRoutingModule
  ]
})
export class AdminModule { }
