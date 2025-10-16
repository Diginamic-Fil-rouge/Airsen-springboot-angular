import { Injectable } from '@angular/core';
import { JwtPayload } from '@/auth/models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly ACCESS_TOKEN_KEY = 'airsen_access_token';
  private readonly REFRESH_TOKEN_KEY = 'airsen_refresh_token';

  constructor() {}

  /**
   * Store access token
   */
  storeAccessToken(token: string): void {
    try {
      localStorage.setItem(this.ACCESS_TOKEN_KEY, token);
    } catch (error) {
      console.error('Failed to store access token:', error);
    }
  }

  /**
   * Store refresh token
   */
  storeRefreshToken(token: string): void {
    try {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
    } catch (error) {
      console.error('Failed to store refresh token:', error);
    }
  }

  /**
   * Get stored access token
   */
  getAccessToken(): string | null {
    try {
      return localStorage.getItem(this.ACCESS_TOKEN_KEY);
    } catch (error) {
      console.error('Failed to retrieve access token:', error);
      return null;
    }
  }

  /**
   * Get stored refresh token
   */
  getRefreshToken(): string | null {
    try {
      return localStorage.getItem(this.REFRESH_TOKEN_KEY);
    } catch (error) {
      console.error('Failed to retrieve refresh token:', error);
      return null;
    }
  }

  /**
   * Clear all stored tokens
   */
  clearTokens(): void {
    try {
      localStorage.removeItem(this.ACCESS_TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    } catch (error) {
      console.error('Failed to clear tokens:', error);
    }
  }

  /**
   * Check if access token exists and is valid
   */
  isTokenValid(): boolean {
    const token = this.getAccessToken();
    return token ? !this.isTokenExpired(token) : false;
  }

  /**
   * Check if token is expired
   */
  isTokenExpired(token: string): boolean {
    try {
      const payload = this.decodeToken(token);
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }

  /**
   * Decode JWT token payload
   */
  decodeToken(token: string): JwtPayload {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch (error) {
      throw new Error('Invalid JWT token format');
    }
  }

  /**
   * Get user role from token
   */
  getUserRoleFromToken(): string | null {
    try {
      const token = this.getAccessToken();
      if (token && !this.isTokenExpired(token)) {
        const payload = this.decodeToken(token);
        return payload.role;
      }
      return null;
    } catch {
      return null;
    }
  }

  /**
   * Get user ID from token
   */
  getUserIdFromToken(): number | null {
    try {
      const token = this.getAccessToken();
      if (token && !this.isTokenExpired(token)) {
        const payload = this.decodeToken(token);
        return payload.userId;
      }
      return null;
    } catch {
      return null;
    }
  }

  /**
   * Calculate time until token expires (in seconds)
   */
  getTokenExpirationTime(): number | null {
    try {
      const token = this.getAccessToken();
      if (token) {
        const payload = this.decodeToken(token);
        const expirationTime = payload.exp * 1000;
        const currentTime = Date.now();
        return Math.max(0, Math.floor((expirationTime - currentTime) / 1000));
      }
      return null;
    } catch {
      return null;
    }
  }
}