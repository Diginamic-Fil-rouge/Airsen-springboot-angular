// Authentication Models based on backend JWT implementation

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  userEmail: string;
  userFirstName: string;
  userLastName: string;
  userRole: string;
  issuedAt: string;
  fullUserName: string;
  expired: boolean;
}

export interface AuthUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  commune?: string;
  department?: string;
  region?: string;
  // Champs supplémentaires pour le profil
  address?: string;
  phone?: string;
  bio?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role?: string; // Optional, defaults to USER on backend
}

export interface RegisterResponse {
  message: string;
  user: AuthUser;
}

export interface JwtPayload {
  sub: string; // user email
  userId: number;
  role: string;
  iat: number; // issued at
  exp: number; // expiration time
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetConfirmRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}