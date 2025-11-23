import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject,
  signal
} from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { ExportDataService } from '@/core/services/export-data.service';
import { ExportRecord } from '@/shared/models/export.model';

/**
 * ExportHistoryComponent
 *
 * Displays user's export history with download links and metadata
 * Integrated into the Export page to show previous exports
 *
 * Features:
 * - List of previous exports with metadata (date, location, file size, format)
 * - Chronological sorting (newest first)
 * - File format icons and size formatting
 * - Empty state when no exports exist
 * - Refresh functionality
 *
 * Integration:
 * - Used in ExportPageComponent
 * - Uses ExportDataService for history retrieval
 * - Local storage based (client-side tracking)
 */
@Component({
  standalone: false,
  selector: 'app-export-history',
  templateUrl: './export-history.component.html',
  styleUrls: ['./export-history.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExportHistoryComponent implements OnInit {
  // Reactive signals for state management
  exportHistory = signal<ExportRecord[]>([]);
  isLoading = signal<boolean>(false);
  error = signal<string | null>(null);

  private exportDataService = inject(ExportDataService);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.loadExportHistory();
  }

  /**
   * Load export history from service
   * Uses localStorage for client-side export tracking
   */
  loadExportHistory(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.cdr.markForCheck();

    this.exportDataService.getExportHistory().subscribe({
      next: (records: ExportRecord[]) => {
        this.exportHistory.set(records);
        this.isLoading.set(false);
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.error.set(err.message || 'Failed to load export history');
        this.isLoading.set(false);
        this.cdr.markForCheck();
      }
    });
  }

  /**
   * Refresh export history
   */
  refreshHistory(): void {
    this.loadExportHistory();
    this.showSuccess('Export history refreshed');
  }

  /**
   * Clear all export history
   */
  clearHistory(): void {
    if (this.exportHistory().length === 0) {
      this.showError('No export history to clear');
      return;
    }

    // Clear localStorage
    localStorage.removeItem('airsen_export_history');
    this.exportHistory.set([]);
    this.showSuccess('Export history cleared');
    this.cdr.markForCheck();
  }

  /**
   * Format file size in human-readable format
   */
  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  }

  /**
   * Get format icon based on export format
   */
  getFormatIcon(format: string): string {
    switch (format.toUpperCase()) {
      case 'CSV': return 'table_chart';
      case 'PDF': return 'picture_as_pdf';
      default: return 'file_download';
    }
  }

  /**
   * Get format color class for styling
   */
  getFormatColorClass(format: string): string {
    switch (format.toUpperCase()) {
      case 'CSV': return 'format-csv';
      case 'PDF': return 'format-pdf';
      default: return 'format-default';
    }
  }

  /**
   * Format relative time (e.g., "2 hours ago")
   * Accepts both Date objects and ISO date strings
   */
  formatRelativeTime(date: Date | string): string {
    const now = new Date();
    const exportDate = date instanceof Date ? date : new Date(date);
    const diffMs = now.getTime() - exportDate.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor(diffMs / (1000 * 60));

    if (diffDays > 0) {
      return `${diffDays} jour${diffDays > 1 ? 's' : ''} ago`;
    } else if (diffHours > 0) {
      return `${diffHours} heure${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffMinutes > 0) {
      return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else {
      return 'À l\'instant';
    }
  }

  /**
   * TrackBy function for ngFor performance optimization
   */
  trackByRecordId(index: number, record: ExportRecord): string {
    return record.id;
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['error-snackbar']
    });
  }
}