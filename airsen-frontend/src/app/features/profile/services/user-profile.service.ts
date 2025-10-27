import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserDTO } from "../models/user.model";
import { UpdateUserProfileRequest } from "../models/update-user-profile-request.model";


// DTO pour changer le mot de passe
export interface UpdatePasswordRequest {
  currentPassword: string;
  newPassword: string;
}


@Injectable({
  providedIn: 'root',
})
export class UserProfileService {
  private apiUrl = 'http://localhost:8080/api/v1/users';

  constructor(private http: HttpClient) {}

  // Récupérer le profil de l'utilisateur connecté
  getProfile(): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.apiUrl}/profile`);
  }

  // Mettre à jour les informations du profil
  updateProfile(data: UpdateUserProfileRequest): Observable<UserDTO> {
    return this.http.put<UserDTO>(`${this.apiUrl}/profile`, data);
  }

  // Changer le mot de passe
  updatePassword(data: UpdatePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/password`, data);
  }
}
