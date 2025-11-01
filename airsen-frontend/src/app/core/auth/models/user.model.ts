import { Commune } from '@/shared/models';

/**
 * Core User model representing authenticated user with profile information
 * Maps to backend User entity
 */
export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  telephone?: string;
  address?: string;
  bio?: string;
  avatarUrl?: string;
  role: UserRole;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * User role enumeration for authorization
 * VISITOR: Public access, no authentication required
 * USER: Authenticated user with standard permissions
 * ADMIN: Administrative user with elevated permissions
 */
export enum UserRole {
  VISITOR = "VISITOR",
  USER = "USER",
  ADMIN = "ADMIN",
}

/**
 * Request model for user registration
 */
export interface UserRegistrationRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  telephone?: string;
  address?: string;
}

/**
 * Request model for updating user profile
 */
export interface UserUpdateRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  telephone?: string;
  address?: string;
  bio?: string;
  avatarUrl?: string;
}

/**
 * Request model for changing user password
 */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * User profile with extended information including preferences
 */
export interface UserProfile extends User {
  favoriteCommunes: Commune[];
  notificationPreferences: NotificationPreference[];
}

/**
 * User notification preferences configuration
 */
export interface NotificationPreference {
  id: number;
  userId: number;
  airQualityAlerts: boolean;
  weatherAlerts: boolean;
  emailNotifications: boolean;
  scope: NotificationScope;
}

/**
 * Enumeration for notification scope (geographic coverage)
 */
export enum NotificationScope {
  COMMUNE = "COMMUNE",
  DEPARTMENT = "DEPARTMENT",
  REGION = "REGION",
  FRANCE = "FRANCE",
}
