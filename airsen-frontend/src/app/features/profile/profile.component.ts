import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '@/core/auth/services/auth.service';
import { AlertService } from '@/core/services/alert.service';
import { UserProfileService } from './services/user-profile.service';
import { AuthUser } from '@/core/auth/models/auth.model';
import {
  UserProfile,
  UpdateProfileRequest,
  ChangePasswordRequest,
  NotificationSettings
} from '@/shared/models/profile.model';
import { CampaignNotification } from '@/shared/models';

/**
 * ProfileComponent - User Profile Management (Refactored)
 *
 * Smart container component orchestrating 3 child components:
 * - Tab 1 (Informations personnelles): InfoFormComponent
 * - Tab 2 (Sécurité): ChangePasswordComponent
 * - Tab 3 (Notifications): Received notifications + NotificationToggleComponent
 *
 * Architecture:
 * - Uses Angular Material mat-tab-group for tab management
 * - Event-driven: Listens to child component events
 * - RxJS: Proper subscription management with takeUntil
 * - Feedback: MatSnackBar for success/error messages (no modal dialogs)
 */
@Component({
  standalone: false,
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit, OnDestroy {
  currentUser: AuthUser | null = null;
  userProfile: UserProfile | null = null;
  receivedNotifications: CampaignNotification[] = [];

  isLoadingProfile = true;
  isLoadingNotifications = true;
  isUpdatingProfile = false;
  isChangingPassword = false;

  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private alertService: AlertService,
    private userProfileService: UserProfileService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Loads current authenticated user and their profile.
   */
  private loadCurrentUser(): void {
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.currentUser = user;
          if (user) {
            this.loadUserProfile();
            this.loadReceivedNotifications(user.id);
          }
        },
        error: (error) => {
          console.error('Error loading current user:', error);
          this.showError('Erreur lors du chargement de l\'utilisateur');
          this.isLoadingProfile = false;
        }
      });
  }

  /**
   * Loads user profile from backend.
   */
  private loadUserProfile(): void {
    this.isLoadingProfile = true;

    this.userProfileService.getProfile()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (profile) => {
          this.userProfile = profile;
          this.isLoadingProfile = false;
        },
        error: (error) => {
          console.error('Error loading user profile:', error);
          this.showError('Impossible de charger le profil');
          this.isLoadingProfile = false;
        }
      });
  }

  /**
   * Loads notifications received by user (admin broadcasts).
   */
  private loadReceivedNotifications(userId: number): void {
    this.isLoadingNotifications = true;

    this.alertService.getUserNotifications(userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (notifications) => {
          this.receivedNotifications = notifications;
          this.isLoadingNotifications = false;
        },
        error: (error) => {
          console.error('Error loading notifications:', error);
          // Don't show error - notifications tab will show empty state
          this.isLoadingNotifications = false;
        }
      });
  }

  /**
   * Handles profile update event from InfoFormComponent.
   */
  onProfileUpdate(updateRequest: UpdateProfileRequest): void {
    if (!this.currentUser) return;

    this.isUpdatingProfile = true;

    this.userProfileService.updateProfile(updateRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isUpdatingProfile = false;

          if (response.success && response.user) {
            this.userProfile = response.user;
            this.showSuccess('Profil mis à jour avec succès !');
          } else {
            this.showError(response.error || 'Erreur lors de la mise à jour du profil');
          }
        },
        error: (error) => {
          this.isUpdatingProfile = false;
          console.error('Error updating profile:', error);
          this.showError('Erreur lors de la sauvegarde du profil');
        }
      });
  }

  /**
   * Handles password change event from ChangePasswordComponent.
   */
  onPasswordChange(changeRequest: ChangePasswordRequest): void {
    if (!this.currentUser) return;

    this.isChangingPassword = true;

    this.userProfileService.changePassword(changeRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isChangingPassword = false;

          if (response.success) {
            this.showSuccess('Mot de passe modifié avec succès !');
            // Optionally force re-login for security
            // this.authService.logout();
          } else {
            this.showError(response.error || 'Erreur lors du changement de mot de passe');
          }
        },
        error: (error) => {
          this.isChangingPassword = false;
          console.error('Error changing password:', error);

          // Handle specific error messages from backend
          if (error.status === 401) {
            this.showError('Mot de passe actuel incorrect');
          } else if (error.status === 400) {
            this.showError('Le nouveau mot de passe ne respecte pas les critères de sécurité');
          } else {
            this.showError('Erreur lors du changement de mot de passe');
          }
        }
      });
  }

  /**
   * Handles notification settings change from NotificationToggleComponent.
   * Settings are stored in localStorage (no backend call needed).
   */
  onNotificationSettingsChange(settings: NotificationSettings): void {
    console.log('Notification settings updated:', settings);
    this.showSuccess('Préférences de notification enregistrées');
  }

  /**
   * Gets user initials for avatar display (e.g., "SP" for "Sarah Pham").
   */
  get userInitials(): string {
    if (!this.userProfile) return 'AA';

    const first = this.userProfile.firstName?.trim().charAt(0).toUpperCase() || '';
    const last = this.userProfile.lastName?.trim().charAt(0).toUpperCase() || '';
    const initials = `${first}${last}`.trim();

    return initials || 'AA';
  }

  /**
   * Gets user full name.
   */
  get userFullName(): string {
    if (!this.userProfile) return 'Utilisateur';
    return `${this.userProfile.firstName} ${this.userProfile.lastName}`;
  }

  /**
   * Gets count of unread notifications.
   */
  get unreadNotificationCount(): number {
    return this.receivedNotifications.filter(n => n.status === 'PENDING').length;
  }

  /**
   * Shows success message using MatSnackBar.
   */
  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }

  /**
   * Shows error message using MatSnackBar.
   */
  private showError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top'
    });
  }
}
