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
 * - Quick export button for PDF snapshots
 * - Favorite button for saving communes
 *
 * Note: Advanced exports (CSV with date range) are handled in the dedicated Export page.
 * See: /export route for comprehensive export functionality.
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

  private cdr = inject(ChangeDetectorRef);
}
