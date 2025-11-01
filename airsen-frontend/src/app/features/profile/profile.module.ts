import { NgModule } from "@angular/core";
import { ProfileRoutingModule } from "./profile-routing.module";
import { SharedModule } from "@/shared/shared.module";

// Container Component
import { ProfileComponent } from "./profile.component";

// Child Components
import { InfoFormComponent } from "./components/info-form/info-form.component";
import { ChangePasswordComponent } from "./components/change-password/change-password.component";
import { NotificationToggleComponent } from "./components/notification-toggle/notification-toggle.component";

/**
 * ProfileModule - User Profile Management Feature Module
 *
 * This module implements the refactored profile feature with:
 * - ProfileComponent: Smart container with mat-tab-group
 * - InfoFormComponent: Personal information editing
 * - ChangePasswordComponent: Password change with strength validation
 * - NotificationToggleComponent: Notification preferences (localStorage)
 *
 * Architecture:
 * - Event-driven child-parent communication
 * - Reactive forms with custom validators
 * - MatSnackBar for user feedback (no modal dialogs)
 * - Initials-based avatar (no image upload)
 */
@NgModule({
  declarations: [
    // Container
    ProfileComponent,
    // Child Components
    InfoFormComponent,
    ChangePasswordComponent,
    NotificationToggleComponent
  ],
  imports: [
    ProfileRoutingModule,
    SharedModule 
  ],
})
export class ProfileModule {}
