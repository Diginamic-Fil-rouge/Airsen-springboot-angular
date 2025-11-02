import { Commune } from './commune.model';

/**
 * Represents a favorite commune with geographic hierarchy
 * Used for displaying favorites list
 */
export interface UserFavoriteResponse {
  communeInseeCode: string;      // 5-digit INSEE code
  communeName: string;            // e.g., "Paris 16e Arrondissement"
  departmentName: string;         // e.g., "Paris"
  regionName: string;             // e.g., "Île-de-France"
  addedAt: string;                // ISO 8601 datetime
}

/**
 * Legacy interface - kept for backward compatibility
 * @deprecated Use UserFavoriteResponse instead
 */
export interface Favorite {
  communeInseeCode: string;
  communeName: string;
  departmentName: string;
  regionName: string;
  addedAt: Date;
}

/**
 * FavoriteCheckResponse
 * Response for checking if a commune is favorited
 */
export interface FavoriteCheckResponse {
  isFavorited: boolean;
}

/**
 * AddFavoriteRequest
 * Request to add a commune to favorites
 */
export interface AddFavoriteRequest {
  communeInseeCode: string;      // Must be 5 digits (validated by backend @Pattern)
}

/**
 * Legacy interface - kept for backward compatibility
 * @deprecated Use AddFavoriteRequest instead
 */
export interface CreateFavoriteRequest {
  inseeCode: string;
}

/**
 * FavoriteCountResponse
 * Current favorites count with maximum limit
 */
export interface FavoriteCountResponse {
  count: number;                  // Current count (0-10)
  maximum: number;                // Always 10 (backend limit)
}

/**
 * FavoriteErrorResponse - Standardized error response from backend
 * Matches backend exception handling patterns
 */
export interface FavoriteErrorResponse {
  message: string;
  code: string;
  timestamp: string;
  path: string;
  status: number;
}

/**
 * FavoriteErrorCode - Enumeration of possible favorite-related errors
 * Based on backend exception types and validation rules
 */
export enum FavoriteErrorCode {
  MAX_FAVORITES_REACHED = 'MAX_FAVORITES_REACHED',
  INVALID_INSEE_CODE = 'INVALID_INSEE_CODE',
  COMMUNE_NOT_FOUND = 'COMMUNE_NOT_FOUND',
  FAVORITE_NOT_FOUND = 'FAVORITE_NOT_FOUND',
  ALREADY_FAVORITED = 'ALREADY_FAVORITED',
  UNAUTHORIZED_ACCESS = 'UNAUTHORIZED_ACCESS',
  FORBIDDEN_ACCESS = 'FORBIDDEN_ACCESS'
}

/**
 * Legacy interface - kept for backward compatibility
 * Represents internal user favorite entity
 */
export interface UserFavorite {
  id: number;
  user: {
    id: number;
  };
  commune: Commune;
  addedAt: Date;
}
