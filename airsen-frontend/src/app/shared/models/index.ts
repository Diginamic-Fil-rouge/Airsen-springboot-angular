/**
 * Shared Models Barrel Export
 *
 * This file provides a centralized export for all shared TypeScript models
 * used across the AIRSEN Angular application.
 *
 * Usage:
 * import { Alert, Commune, AirQualityData } from '@/shared/models';
 */

// Alert & Notification System Models (Admin-Centric)
export * from './alert.model';

// Air Quality Models
export * from './air-quality.model';

// Geographic Models (Commune, Department, Region)
export * from './commune.model';

// Favorite Models
export * from './favorite.model';

// Weather Models
export * from './weather.model';

// Export Models
export * from './export.model';

// API Response Models
export * from './api-response.model';

// User Profile Models
export * from './profile.model';

// Re-export Auth models for convenience (from core/auth/models)
export { User, UserRole, UserRegistrationRequest, UserUpdateRequest, NotificationPreference, NotificationScope } from '../../core/auth/models/user.model';
export * from '../../core/auth/models/auth.model';
