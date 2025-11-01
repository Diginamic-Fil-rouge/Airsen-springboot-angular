import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { NotificationSettings, DEFAULT_NOTIFICATION_SETTINGS } from '@/shared/models/profile.model';

/**
 * NotificationToggleComponent - Notification Preferences
 *
 * Displays 4 Material slide toggles for user notification preferences:
 * - Email Alerts: Receive email for admin broadcast alerts
 * - Browser Notifications: Show browser push notifications (future)
 * - Forum Replies: Notify when someone replies to user's forum post
 * - Favorite Alerts: Notify when favorite commune has alert (future)
 *
 * Settings stored in localStorage (no backend persistence).
 * Storage Key: `airsen_notification_settings_{userId}`
 *
 * Emits settingsChange event when any toggle is changed.
 */
@Component({
  standalone: false,
  selector: 'app-notification-toggle',
  templateUrl: './notification-toggle.component.html',
  styleUrls: ['./notification-toggle.component.scss']
})
export class NotificationToggleComponent implements OnInit {
  @Input() userId!: number;
  @Output() settingsChange = new EventEmitter<NotificationSettings>();

  settings: NotificationSettings = {
    userId: 0,
    emailAlerts: DEFAULT_NOTIFICATION_SETTINGS.emailAlerts,
    browserNotifications: DEFAULT_NOTIFICATION_SETTINGS.browserNotifications,
    forumReplies: DEFAULT_NOTIFICATION_SETTINGS.forumReplies,
    favoriteAlerts: DEFAULT_NOTIFICATION_SETTINGS.favoriteAlerts,
    updatedAt: new Date()
  };

  // Expose window object for template access
  window: Window = window;

  private readonly STORAGE_KEY_PREFIX = 'airsen_notification_settings_';

  ngOnInit(): void {
    this.loadSettings();
  }

  /**
   * Loads notification settings from localStorage.
   * If no settings exist, uses default values.
   */
  private loadSettings(): void {
    const storageKey = `${this.STORAGE_KEY_PREFIX}${this.userId}`;
    const stored = localStorage.getItem(storageKey);

    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        this.settings = {
          ...parsed,
          userId: this.userId,
          updatedAt: new Date(parsed.updatedAt)
        };
      } catch (error) {
        console.error('Error parsing notification settings:', error);
        this.settings = this.getDefaultSettings();
      }
    } else {
      this.settings = this.getDefaultSettings();
    }
  }

  /**
   * Saves notification settings to localStorage.
   */
  private saveSettings(): void {
    const storageKey = `${this.STORAGE_KEY_PREFIX}${this.userId}`;
    this.settings.updatedAt = new Date();

    try {
      localStorage.setItem(storageKey, JSON.stringify(this.settings));
      this.settingsChange.emit(this.settings);
    } catch (error) {
      console.error('Error saving notification settings:', error);
    }
  }

  /**
   * Returns default notification settings for user.
   */
  private getDefaultSettings(): NotificationSettings {
    return {
      userId: this.userId,
      ...DEFAULT_NOTIFICATION_SETTINGS,
      updatedAt: new Date()
    };
  }

  /**
   * Handles toggle change for email alerts.
   */
  onEmailAlertsChange(enabled: boolean): void {
    this.settings.emailAlerts = enabled;
    this.saveSettings();
  }

  /**
   * Handles toggle change for browser notifications.
   */
  onBrowserNotificationsChange(enabled: boolean): void {
    this.settings.browserNotifications = enabled;
    this.saveSettings();

    // Request browser permission if enabled (future implementation)
    if (enabled && 'Notification' in window) {
      Notification.requestPermission().then(permission => {
        if (permission !== 'granted') {
          // Revert toggle if permission denied
          this.settings.browserNotifications = false;
          this.saveSettings();
        }
      });
    }
  }

  /**
   * Handles toggle change for forum replies.
   */
  onForumRepliesChange(enabled: boolean): void {
    this.settings.forumReplies = enabled;
    this.saveSettings();
  }

  /**
   * Handles toggle change for favorite alerts.
   */
  onFavoriteAlertsChange(enabled: boolean): void {
    this.settings.favoriteAlerts = enabled;
    this.saveSettings();
  }

  /**
   * Resets all settings to default values.
   */
  resetToDefaults(): void {
    this.settings = this.getDefaultSettings();
    this.saveSettings();
  }
}
