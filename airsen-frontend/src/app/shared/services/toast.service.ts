import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface Toast {
  id: number;
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
  duration?: number;
}

/**
 * ToastService
 *
 * Centralized service for managing toast notifications.
 * Provides methods to show success, error, info, and warning toasts.
 */
@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastSubject = new Subject<Toast>();
  private toastIdCounter = 0;

  toasts$ = this.toastSubject.asObservable();

  /**
   * Show a success toast notification
   */
  success(message: string, duration: number = 3000): void {
    this.show('success', message, duration);
  }

  /**
   * Show an error toast notification
   */
  error(message: string, duration: number = 5000): void {
    this.show('error', message, duration);
  }

  /**
   * Show an info toast notification
   */
  info(message: string, duration: number = 3000): void {
    this.show('info', message, duration);
  }

  /**
   * Show a warning toast notification
   */
  warning(message: string, duration: number = 4000): void {
    this.show('warning', message, duration);
  }

  /**
   * Show a toast notification
   */
  private show(type: Toast['type'], message: string, duration: number): void {
    const toast: Toast = {
      id: ++this.toastIdCounter,
      type,
      message,
      duration
    };
    this.toastSubject.next(toast);
  }
}
