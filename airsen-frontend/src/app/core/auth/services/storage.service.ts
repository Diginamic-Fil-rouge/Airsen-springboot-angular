import { Injectable } from '@angular/core';
import { AuthUser } from '@/auth/models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class StorageService {
  private readonly USER_KEY = 'airsen_user';
  private readonly RETURN_URL_KEY = 'airsen_return_url';

  constructor() {}

  /**
   * Store user data
   */
  storeUser(user: AuthUser): void {
    try {
      localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    } catch (error) {
      console.error('Failed to store user data:', error);
    }
  }

  /**
   * Get stored user data
   */
  getStoredUser(): AuthUser | null {
    try {
      const userStr = localStorage.getItem(this.USER_KEY);
      return userStr ? JSON.parse(userStr) : null;
    } catch (error) {
      console.error('Failed to retrieve user data:', error);
      return null;
    }
  }

  /**
   * Clear stored user data
   */
  clearUser(): void {
    try {
      localStorage.removeItem(this.USER_KEY);
    } catch (error) {
      console.error('Failed to clear user data:', error);
    }
  }

  /**
   * Store return URL for redirect after login
   */
  storeReturnUrl(url: string): void {
    try {
      localStorage.setItem(this.RETURN_URL_KEY, url);
    } catch (error) {
      console.error('Failed to store return URL:', error);
    }
  }

  /**
   * Get stored return URL
   */
  getReturnUrl(): string | null {
    try {
      return localStorage.getItem(this.RETURN_URL_KEY);
    } catch (error) {
      console.error('Failed to retrieve return URL:', error);
      return null;
    }
  }

  /**
   * Clear stored return URL
   */
  clearReturnUrl(): void {
    try {
      localStorage.removeItem(this.RETURN_URL_KEY);
    } catch (error) {
      console.error('Failed to clear return URL:', error);
    }
  }

  /**
   * Clear all stored data
   */
  clearAll(): void {
    this.clearUser();
    this.clearReturnUrl();
  }

  /**
   * Check if storage is available
   */
  isStorageAvailable(): boolean {
    try {
      const test = '__storage_test__';
      localStorage.setItem(test, test);
      localStorage.removeItem(test);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get storage usage information (for debugging)
   */
  getStorageInfo(): { used: number; available: boolean } {
    try {
      let used = 0;
      for (let key in localStorage) {
        if (localStorage.hasOwnProperty(key)) {
          used += localStorage[key].length + key.length;
        }
      }
      return {
        used: used,
        available: this.isStorageAvailable()
      };
    } catch {
      return {
        used: 0,
        available: false
      };
    }
  }
}