import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  inject,
} from "@angular/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { MatSnackBar } from "@angular/material/snack-bar";

import { ExportDataService } from "@/core/services/export-data.service";
import { AuthService } from "@/core/auth/services/auth.service";
import { AuthUser } from "@/core/auth/models/auth.model";

/**
 * QuickExportButtonComponent
 *
 * One-click environmental data export button with advanced options toggle
 *
 * Default behavior:
 * - Format: PDF (snapshot data)
 * - Type: COMBINED (air quality + weather)
 * - Date range: Last 7 days
 *
 * Requirements:
 * - Authentication required (USER or ADMIN role)
 * - Valid commune INSEE code (5 digits)
 *
 * Integration with ExportDataService:
 * - Uses exportCurrentData() for quick PDF export (current snapshot)
 * - Parent component handles advanced export options (CSV, custom date ranges)
 * - Export history is automatically saved to localStorage (max 50 records)
 */
@Component({
  standalone: false,
  selector: "app-quick-export-button",
  templateUrl: "./quick-export-button.component.html",
  styleUrls: ["./quick-export-button.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuickExportButtonComponent implements OnInit, OnDestroy {
  @Input() communeInseeCode!: string; // 5-digit INSEE code (REQUIRED)
  @Input() communeName!: string; // Commune name for display (REQUIRED)
  @Input() size: "small" | "medium" | "large" = "medium";
  @Output() advancedExportRequested = new EventEmitter<void>();

  isExporting = false;
  showAdvancedPanel = false;
  currentUser?: AuthUser | null;

  private readonly destroy$ = new Subject<void>();

  private exportService = inject(ExportDataService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    // Validate required inputs
    if (!this.communeInseeCode || !/^\d{5}$/.test(this.communeInseeCode)) {
      console.warn("QuickExportButtonComponent: invalid or missing communeInseeCode");
    }
    if (!this.communeName) {
      console.warn("QuickExportButtonComponent: missing communeName");
    }

    // Subscribe to authentication state
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.currentUser = user;
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Quick export with defaults:
   * - Format: PDF
   * - Type: COMBINED (air quality + weather)
   * - Data: Current snapshot (not historical)
   *
   * Flow:
   * 1. Check authentication (USER or ADMIN required)
   * 2. Validate inputs (INSEE code, commune name)
   * 3. Call ExportDataService.exportCurrentData()
   * 4. Service fetches data from GET /api/v1/communes/{inseeCode}/export-data
   * 5. Service generates PDF with jsPDF
   * 6. Service auto-downloads file and saves to localStorage history
   * 7. Show success/error notification
   */
  quickExport(): void {
    // Authentication check - USER or ADMIN only
    if (!this.currentUser || !["USER", "ADMIN"].includes(this.currentUser.role)) {
      this.snackBar.open("Veuillez vous connecter pour exporter les données", "Fermer", {
        duration: 5000,
        horizontalPosition: "center",
        verticalPosition: "top",
      });
      return;
    }

    // Input validation
    if (!this.communeInseeCode || !/^\d{5}$/.test(this.communeInseeCode)) {
      console.error("QuickExportButtonComponent: invalid communeInseeCode");
      this.snackBar.open("Code INSEE invalide", "Fermer", { duration: 5000 });
      return;
    }

    if (!this.communeName) {
      console.error("QuickExportButtonComponent: missing communeName");
      this.snackBar.open("Nom de commune manquant", "Fermer", { duration: 5000 });
      return;
    }

    // Start export
    this.isExporting = true;
    this.cdr.markForCheck();

    this.exportService
      .exportCurrentData(this.communeInseeCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (record) => {
          this.isExporting = false;
          const fileSize = this.formatFileSize(record.fileSize);
          this.snackBar.open(`Exporté : ${record.format} (${fileSize})`, "Fermer", {
            duration: 3500,
            horizontalPosition: "center",
            verticalPosition: "top",
          });
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          this.isExporting = false;
          const message = err instanceof Error ? err.message : "Erreur lors de l'export des données";
          this.snackBar.open(`Erreur d'export : ${message}`, "Fermer", {
            duration: 5000,
            horizontalPosition: "center",
            verticalPosition: "top",
          });
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Toggles advanced export panel visibility
   * Emits event for parent component to handle advanced options
   */
  toggleAdvancedPanel(): void {
    this.showAdvancedPanel = !this.showAdvancedPanel;
    if (this.showAdvancedPanel) {
      this.advancedExportRequested.emit();
    }
    this.cdr.markForCheck();
  }

  /**
   * Check if user can export (USER or ADMIN role)
   */
  get canExport(): boolean {
    return this.currentUser ? ["USER", "ADMIN"].includes(this.currentUser.role) : false;
  }

  /**
   * Format file size in bytes to human-readable string
   * @param bytes - File size in bytes
   * @returns Formatted string (e.g., "1.5 MB", "245 KB")
   */
  private formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  }
}
