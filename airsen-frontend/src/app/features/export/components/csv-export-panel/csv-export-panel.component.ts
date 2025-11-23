import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  inject,
} from "@angular/core";
import { FormControl, FormGroup, Validators, AbstractControl, ValidationErrors } from "@angular/forms";
import { MatSnackBar } from "@angular/material/snack-bar";

import { ExportDataService } from "@/services/export-data.service";
import { ExportRecord } from "@/shared/models/export.model";
import { UserFavoriteResponse } from "@/shared/models/favorite.model";

/**
 * CsvExportPanelComponent
 *
 * CSV export panel for exporting historical environmental data with custom date ranges
 * Integrated into the Export page for favorite communes
 *
 * Features:
 * - Date range selection with Material date pickers (max 90 days)
 * - CSV time-series export only
 * - User preference persistence in localStorage
 * - Form validation with custom validators
 * - Loading state during export
 * - Success/error feedback via snackbar
 *
 * Integration:
 * - Used in ExportPageComponent for favorite commune CSV exports
 * - Uses ExportDataService for CSV generation
 * - Emits exportComplete event to parent on success
 *
 * Validation Rules:
 * - Start date < End date
 * - Date range <= 90 days (backend limit)
 * - Both dates required
 */
@Component({
  standalone: false,
  selector: "app-csv-export-panel",
  templateUrl: "./csv-export-panel.component.html",
  styleUrls: ["./csv-export-panel.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CsvExportPanelComponent implements OnInit {
  @Input() selectedCommune!: UserFavoriteResponse;
  @Output() exportComplete = new EventEmitter<void>();
  @Output() panelClosed = new EventEmitter<void>();

  // Form controls with validators for CSV export
  exportForm = new FormGroup(
    {
      startDate: new FormControl<Date | null>(this.getDefaultStartDate(), [Validators.required]),
      endDate: new FormControl<Date | null>(new Date(), [Validators.required]),
    },
    { validators: [this.dateRangeValidator.bind(this)] }
  );

  isExporting = false;
  maxDate = new Date(); // Today (cannot select future dates)
  minDate = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000); // 90 days ago

  private exportService = inject(ExportDataService);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);

  private readonly PREFERENCES_KEY = "airsen_csv_export_preferences";

  ngOnInit(): void {
    // Load user preferences from localStorage (if exists)
    this.loadUserPreferences();
  }

  /**
   * Export CSV data for the selected commune
   *
   * Flow:
   * 1. Validate form (date range)
   * 2. Call ExportDataService.exportAsCSV() with commune INSEE code and date range
   * 3. Show loading state
   * 4. Save preferences to localStorage
   * 5. Emit exportComplete event
   * 6. Show success/error notification
   */
  onExport(): void {
    if (this.exportForm.invalid) {
      this.showValidationErrors();
      return;
    }

    const { startDate, endDate } = this.exportForm.value;

    if (!startDate || !endDate || !this.selectedCommune) {
      this.snackBar.open("Veuillez remplir tous les champs requis", "Fermer", { duration: 3000 });
      return;
    }

    this.isExporting = true;
    this.cdr.markForCheck();

    // Export CSV with historical data
    this.exportService.exportAsCSV(
      this.selectedCommune.communeInseeCode,
      this.formatDate(startDate),
      this.formatDate(endDate)
    ).subscribe({
      next: (record: ExportRecord) => {
        this.isExporting = false;
        this.saveUserPreferences();
        const fileSize = this.formatFileSize(record.fileSize);
        const dateRange = `${this.formatDate(startDate)} - ${this.formatDate(endDate)}`;
        this.snackBar.open(
          `Exporté CSV pour ${this.selectedCommune.communeName} : ${fileSize} (${dateRange})`,
          "Fermer",
          { duration: 4000 }
        );
        this.exportComplete.emit();
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.isExporting = false;
        const message = err instanceof Error ? err.message : "Une erreur est survenue lors de l'export CSV";
        this.snackBar.open(`Erreur : ${message}`, "Fermer", { duration: 5000 });
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Close the CSV export panel
   */
  onClose(): void {
    this.panelClosed.emit();
  }

  /**
   * Custom validator: Date range validation
   *
   * Rules:
   * 1. Start date must be before end date
   * 2. Date range cannot exceed 90 days (backend limit)
   */
  private dateRangeValidator(group: AbstractControl): ValidationErrors | null {
    const startDate = group.get("startDate")?.value as Date | null;
    const endDate = group.get("endDate")?.value as Date | null;

    if (!startDate || !endDate) return null;

    // Check if start date is after end date
    if (startDate > endDate) {
      return {
        dateRangeInvalid: "La date de début doit être antérieure à la date de fin",
      };
    }

    // Check 90-day limit
    const diffDays = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays > 90) {
      return {
        dateRangeTooLarge: "La plage de dates ne peut pas dépasser 90 jours",
      };
    }

    return null;
  }

  /**
   * Show validation errors with user-friendly messages
   */
  private showValidationErrors(): void {
    const errors = this.exportForm.errors;
    let message = "Veuillez corriger les erreurs dans le formulaire";

    if (errors) {
      if (errors["dateRangeInvalid"]) {
        message = errors["dateRangeInvalid"];
      } else if (errors["dateRangeTooLarge"]) {
        message = errors["dateRangeTooLarge"];
      }
    }

    this.snackBar.open(message, "Fermer", { duration: 4000 });
  }

  /**
   * Get default start date (7 days ago)
   */
  private getDefaultStartDate(): Date {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return date;
  }

  /**
   * Load user preferences from localStorage
   *
   * Preferences stored:
   * - Date range preferences
   * - Last used date range is remembered for convenience
   */
  private loadUserPreferences(): void {
    try {
      const prefs = localStorage.getItem(this.PREFERENCES_KEY);
      if (prefs) {
        const { startDateOffset, endDateOffset } = JSON.parse(prefs);
        if (startDateOffset !== undefined && endDateOffset !== undefined) {
          const startDate = new Date();
          startDate.setDate(startDate.getDate() + startDateOffset);
          const endDate = new Date();
          endDate.setDate(endDate.getDate() + endDateOffset);

          this.exportForm.patchValue({
            startDate: startDate,
            endDate: endDate
          });
        }
      }
    } catch (error) {
      console.warn("Failed to load CSV export preferences:", error);
    }
  }

  /**
   * Save user preferences to localStorage
   */
  private saveUserPreferences(): void {
    try {
      const { startDate, endDate } = this.exportForm.value;
      if (startDate && endDate) {
        const now = new Date();
        const startDateOffset = Math.floor((startDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
        const endDateOffset = Math.floor((endDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
        localStorage.setItem(this.PREFERENCES_KEY, JSON.stringify({
          startDateOffset,
          endDateOffset
        }));
      }
    } catch (error) {
      console.warn("Failed to save CSV export preferences:", error);
    }
  }

  /**
   * Format Date object to YYYY-MM-DD string
   */
  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  /**
   * Format file size in human-readable format
   */
  private formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  }
}