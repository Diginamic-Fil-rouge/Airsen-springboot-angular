import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import {
  UserProfile,
  UpdateProfileRequest,
  ProfileUpdateResponse,
  ChangePasswordRequest
} from '@/shared/models/profile.model';

/**
 * Response from password change endpoint
 */
export interface ChangePasswordResponse {
  success: boolean;
  message?: string;
  error?: string;
}

/**
 * UserProfileService - Enhanced User Profile Management
 *
 * Handles user profile operations:
 * - Get user profile
 * - Update profile information
 * - Change password
 * - (Notification settings managed client-side in localStorage)
 *
 * Backend Endpoints:
 * - GET /api/v1/users/profile - Fetch authenticated user's profile
 * - PUT /api/v1/users/profile - Update profile info
 * - PUT /api/v1/users/password - Change password
 *
 * Improvements from original:
 * - Uses environment.apiUrl instead of hardcoded URL
 * - Returns proper response models (ProfileUpdateResponse, ChangePasswordResponse)
 * - Better error handling with catchError
 * - TypeScript models from shared/models
 */
@Injectable({
  providedIn: 'root'
})
export class UserProfileService {
  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  /**
   * Fetches authenticated user's profile.
   *
   * Backend Endpoint: GET /api/v1/users/profile
   *
   * @returns Observable<UserProfile> - User profile data
   */
  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/profile`).pipe(
      catchError(error => {
        console.error('Error fetching profile:', error);
        throw error;
      })
    );
  }

  /**
   * Updates user profile information.
   *
   * Backend Endpoint: PUT /api/v1/users/profile
   *
   * @param updateRequest - Profile update data (firstName, lastName, telephone, address, bio)
   * @returns Observable<ProfileUpdateResponse> - Update response with success/error
   */
  updateProfile(updateRequest: UpdateProfileRequest): Observable<ProfileUpdateResponse> {
    return this.http.put<UserProfile>(`${this.apiUrl}/profile`, updateRequest).pipe(
      map(updatedUser => ({
        success: true,
        user: updatedUser,
        error: undefined
      })),
      catchError(error => {
        console.error('Error updating profile:', error);

        // Extract error message from backend response
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la mise à jour du profil';

        return of({
          success: false,
          user: undefined,
          error: errorMessage
        });
      })
    );
  }

  /**
   * Changes user password.
   *
   * Backend Endpoint: PUT /api/v1/users/password
   *
   * @param changeRequest - Password change data (currentPassword, newPassword, confirmPassword)
   * @returns Observable<ChangePasswordResponse> - Change response with success/error
   */
  changePassword(changeRequest: ChangePasswordRequest): Observable<ChangePasswordResponse> {
    // Backend expects UpdatePasswordRequest format (currentPassword, newPassword)
    const backendRequest = {
      currentPassword: changeRequest.currentPassword,
      newPassword: changeRequest.newPassword
    };

    return this.http.put<void>(`${this.apiUrl}/password`, backendRequest).pipe(
      map(() => ({
        success: true,
        message: 'Mot de passe modifié avec succès',
        error: undefined
      })),
      catchError(error => {
        console.error('Error changing password:', error);

        // Extract specific error messages from backend
        let errorMessage: string;

        if (error.status === 401) {
          errorMessage = 'Mot de passe actuel incorrect';
        } else if (error.status === 400) {
          errorMessage = error.error?.message || 'Le nouveau mot de passe ne respecte pas les critères de sécurité';
        } else {
          errorMessage = error.error?.message || error.message || 'Erreur lors du changement de mot de passe';
        }

        return of({
          success: false,
          message: undefined,
          error: errorMessage
        });
      })
    );
  }

  /**
   * Deletes user account (future implementation).
   *
   * Backend Endpoint: DELETE /api/v1/users/profile
   *
   * @returns Observable<boolean> - True if deletion successful
   */
  deleteAccount(): Observable<boolean> {
    return this.http.delete<void>(`${this.apiUrl}/profile`).pipe(
      map(() => true),
      catchError(error => {
        console.error('Error deleting account:', error);
        throw error;
      })
    );
  }

  /**
   * Gets user profile completion percentage (calculated client-side).
   *
   * Completion Criteria:
   * - firstName, lastName: +25% each (total 50%)
   * - telephone: +20%
   * - address: +15%
   * - bio: +15%
   *
   * @param profile - User profile data
   * @returns number - Completion percentage (0-100)
   */
  calculateProfileCompletion(profile: UserProfile | null): number {
    if (!profile) return 0;

    let completion = 0;

    // Required fields (50%)
    if (profile.firstName?.trim()) completion += 25;
    if (profile.lastName?.trim()) completion += 25;

    // Optional fields (50%)
    if (profile.telephone?.trim()) completion += 20;
    if (profile.address?.trim()) completion += 15;
    if (profile.bio?.trim()) completion += 15;

    return completion;
  }
}
