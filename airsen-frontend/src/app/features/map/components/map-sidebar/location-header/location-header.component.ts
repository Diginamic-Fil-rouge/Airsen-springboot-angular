import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, inject } from "@angular/core";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";

/**
 * Location Header Component
 *
 * Displays the selected commune's name and metadata in the map sidebar.
 * Shows commune name, department code, region code, and population.
 *
 * Features:
 * - Material icon for location indicator
 * - Formatted population with thousands separator
 * - Department and region codes
 * - Empty state when no commune is selected
 * - Quick export button with advanced export panel toggle
 * - Favorite button for saving communes
 *
 * @example
 * <app-location-header [commune]="selectedCommune"></app-location-header>
 */
@Component({
  standalone: false,
  selector: "app-location-header",
  templateUrl: "./location-header.component.html",
  styleUrls: ["./location-header.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationHeaderComponent {
  /**
   * Selected commune with air quality data
   *
   * Contains commune name, codes, population, and current air quality metrics
   */
  @Input() commune: CommuneWithAirQuality | null = null;

  /**
   * Controls visibility of advanced export panel
   */
  showAdvancedExportPanel = false;

  private cdr = inject(ChangeDetectorRef);

  /**
   * Toggle advanced export panel visibility
   * Triggered by QuickExportButton's "advanced options" action
   */
  onAdvancedExportRequested(): void {
    this.showAdvancedExportPanel = !this.showAdvancedExportPanel;
    this.cdr.markForCheck();
  }

  /**
   * Close advanced export panel
   * Triggered when user closes the panel or completes export
   */
  onAdvancedExportPanelClosed(): void {
    this.showAdvancedExportPanel = false;
    this.cdr.markForCheck();
  }

  /**
   * Handle export completion
   * Called when user successfully exports data from advanced panel
   */
  onExportComplete(): void {
    // Close panel after successful export
    this.showAdvancedExportPanel = false;
    this.cdr.markForCheck();
  }
}
