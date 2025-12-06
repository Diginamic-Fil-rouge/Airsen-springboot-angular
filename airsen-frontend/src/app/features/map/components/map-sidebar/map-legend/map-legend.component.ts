import { ChangeDetectionStrategy, Component } from "@angular/core";

/**
 * AQI Legend Entry
 *
 * Represents one level in the ATMO air quality index scale.
 */
interface LegendEntry {
  /** AQI range and quality label (e.g., "Bon (1-2)") */
  label: string;
  /** Hex color code from design-system.scss */
  color: string;
  /** Minimum ATMO index value for this level */
  min: number;
  /** Maximum ATMO index value for this level */
  max: number;
}

/**
 * Map Legend Component
 *
 * Displays the ATMO air quality index scale with color-coded levels.
 * Used in the map sidebar to help users interpret marker colors.
 *
 * ATMO Index Scale (European Standard):
 * - 1-2: Bon (Good) - Green
 * - 3-4: Moyen (Moderate) - Yellow
 * - 5: Dégradé (Degraded) - Orange
 * - 6: Mauvais (Bad) - Red
 * - 7-8: Très mauvais (Very Bad) - Purple
 * - 9-10: Extrêmement mauvais (Extremely Bad) - Maroon
 *
 * @example
 * <app-map-legend></app-map-legend>
 */
@Component({
  standalone: false,
  selector: "app-map-legend",
  templateUrl: "./map-legend.component.html",
  styleUrls: ["./map-legend.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapLegendComponent {
  /**
   * ATMO Index Legend Entries
   *
   * Colors match design-system.scss variables:
   * - $airsen-green-good: #4CAF50
   * - $airsen-yellow-moderate: #FFC107
   * - $airsen-orange-sensitive: #FF9800
   * - $airsen-red-unhealthy: #F44336
   * - $airsen-purple-very: #9C27B0
   * - $airsen-maroon-hazardous: #8D2635
   */
  readonly legend: LegendEntry[] = [
    { label: "Bon", color: "#4CAF50", min: 1, max: 2 },
    { label: "Moyen", color: "#FFC107", min: 3, max: 4 },
    { label: "Dégradé", color: "#FF9800", min: 5, max: 5 },
    { label: "Mauvais", color: "#F44336", min: 6, max: 6 },
    { label: "Très mauvais", color: "#9C27B0", min: 7, max: 8 },
    { label: "Extrêmement mauvais", color: "#8D2635", min: 9, max: 10 },
  ];
}
