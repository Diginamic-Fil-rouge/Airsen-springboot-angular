/**
 * Profile Models for AIRSEN Angular Application
 *
 * Contains TypeScript models for profile management:
 * - ChangePasswordRequest: Password change request DTO
 * - NotificationSettings: User notification preferences
 * - PasswordStrengthResult: Password strength validation result
 * - UserProfile: Complete user profile data
 */

// ==================== PASSWORD MODELS ====================

/**
 * ChangePasswordRequest - DTO for password change
 *
 * Backend Endpoint: PUT /api/v1/users/password
 */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * PasswordStrengthResult - Client-side password strength validation
 *
 * Strength Levels:
 * - WEAK: < 8 characters, no complexity
 * - FAIR: 8+ characters, single type
 * - GOOD: 8+ characters, 2 types (letters + numbers OR letters + symbols)
 * - STRONG: 8+ characters, 3+ types (uppercase, lowercase, numbers, symbols)
 */
export interface PasswordStrengthResult {
  strength: 'WEAK' | 'FAIR' | 'GOOD' | 'STRONG';
  score: number; // 0-100
  color: string; // Hex color for visual indicator
  message: string; // User-friendly message
  hasMinLength: boolean; // >= 8 characters
  hasUpperCase: boolean; // A-Z
  hasLowerCase: boolean; // a-z
  hasNumbers: boolean; // 0-9
  hasSpecialChars: boolean; // !@#$%^&*
}

// ==================== NOTIFICATION MODELS ====================

/**
 * NotificationSettings - User notification preferences
 *
 * Stored in localStorage (no backend persistence required)
 * Key: `airsen_notification_settings_{userId}`
 */
export interface NotificationSettings {
  userId: number;
  emailAlerts: boolean; // Receive email for admin broadcast alerts
  browserNotifications: boolean; // Show browser push notifications (future)
  forumReplies: boolean; // Notify when someone replies to user's forum post
  favoriteAlerts: boolean; // Notify when favorite commune has alert (future)
  updatedAt: Date;
}

/**
 * Default notification settings for new users
 */
export const DEFAULT_NOTIFICATION_SETTINGS: Omit<NotificationSettings, 'userId' | 'updatedAt'> = {
  emailAlerts: true,
  browserNotifications: false, // Disabled by default (requires permission)
  forumReplies: true,
  favoriteAlerts: true
};

// ==================== USER PROFILE MODELS ====================

//UserProfile - Complete user profile data
export interface UserProfile {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: 'VISITOR' | 'USER' | 'ADMIN';
  telephone?: string;
  address?: string;
  bio?: string;
  createdAt: Date;
  updatedAt: Date;
  isActive: boolean;
}

/**
 * UpdateProfileRequest - DTO for profile update
 *
 * Backend Endpoint: PUT /api/v1/users/profile
 */
export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  telephone?: string;
  address?: string;
  bio?: string;
}

/**
 * ProfileUpdateResponse - Response from profile update
 */
export interface ProfileUpdateResponse {
  success: boolean;
  user?: UserProfile;
  error?: string;
}
