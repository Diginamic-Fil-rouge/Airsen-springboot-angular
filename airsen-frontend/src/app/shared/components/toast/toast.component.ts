import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { ToastService, Toast } from '../../services/toast.service';

/**
 * ToastComponent
 *
 * Displays toast notifications in the top-right corner of the screen.
 * Automatically removes toasts after their duration expires.
 */
@Component({
  selector: 'app-toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss']
})
export class ToastComponent implements OnInit, OnDestroy {
  toasts: Toast[] = [];
  private subscription?: Subscription;

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.subscription = this.toastService.toasts$.subscribe((toast: Toast) => {
      this.toasts.push(toast);

      // Auto-remove toast after duration
      if (toast.duration) {
        setTimeout(() => {
          this.removeToast(toast.id);
        }, toast.duration);
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  removeToast(id: number): void {
    this.toasts = this.toasts.filter(t => t.id !== id);
  }

  getIcon(type: Toast['type']): string {
    switch (type) {
      case 'success': return '';
      case 'error': return '';
      case 'warning': return ' ';
      case 'info': return '9';
      default: return '';
    }
  }
}
