import { Component, Input } from "@angular/core";
import { AirQuality } from "@/shared/models/air-quality.model";

/**
 * AQI Summary Card Component
 *
 * Displays air quality index with:
 * - Dynamic class binding based on ATMO index (.aqi-bg-good, .aqi-bg-moderate, etc.)
 * - Pollutant value null handling (shows "N/A" for missing data)
 * - Data source indicator (Direct/Estimated/Not Available)
 * - Accessibility: ARIA labels for AQI value and pollutants
 * - Color-blind friendly text labels alongside colors
 */
@Component({
  standalone: false,
  selector: "app-aqi-summary-card",
  template: `
    <div
      class="aqi-summary-card"
      [ngClass]="aqiBgClass"
      role="region"
      aria-label="Qualité de l'air"
    >
      <!-- AQI Value Display with ARIA label -->
      <div class="aqi-header">
        <div
          class="aqi-value"
          [attr.aria-label]="aqiAriaLabel"
          role="status"
        >
          {{ displayAqiValue }}
        </div>
        <div class="aqi-label" aria-hidden="true">{{ aqiQualityLabel }}</div>
      </div>

      <!-- Data Source Indicator -->
      <div class="data-source" *ngIf="hasData">
        <mat-icon class="source-icon">{{ dataSourceIcon }}</mat-icon>
        <span class="source-text">{{ dataSourceText }}</span>
      </div>

      <!-- Pollutant Breakdown with null handling -->
      <div class="aqi-breakdown" *ngIf="hasData">
        <div class="pollutant" *ngIf="hasPM25">
          <span class="pollutant-name">PM2.5</span>
          <strong class="pollutant-value" [attr.aria-label]="'PM2.5: ' + displayPM25">{{ displayPM25 }}</strong>
        </div>
        <div class="pollutant" *ngIf="hasPM10">
          <span class="pollutant-name">PM10</span>
          <strong class="pollutant-value" [attr.aria-label]="'PM10: ' + displayPM10">{{ displayPM10 }}</strong>
        </div>
        <div class="pollutant" *ngIf="hasNO2">
          <span class="pollutant-name">NO₂</span>
          <strong class="pollutant-value" [attr.aria-label]="'Dioxyde d\\'azote: ' + displayNO2">{{ displayNO2 }}</strong>
        </div>
        <div class="pollutant" *ngIf="hasO3">
          <span class="pollutant-name">O₃</span>
          <strong class="pollutant-value" [attr.aria-label]="'Ozone: ' + displayO3">{{ displayO3 }}</strong>
        </div>
        <div class="pollutant" *ngIf="hasSO2">
          <span class="pollutant-name">SO₂</span>
          <strong class="pollutant-value" [attr.aria-label]="'Dioxyde de soufre: ' + displaySO2">{{ displaySO2 }}</strong>
        </div>
      </div>

      <!-- No Data State -->
      <div class="no-data" *ngIf="!hasData">
        <mat-icon>info</mat-icon>
        <p>Données de qualité de l'air indisponibles</p>
      </div>
    </div>
  `,
})
export class AqiSummaryCardComponent {
  @Input() airQuality: AirQuality | null = null;

  /**
   * Dynamic background class based on ATMO index
   * Follows AIRSEN design-system.scss color palette
   */
  get aqiBgClass(): string {
    const idx = this.airQuality?.globalIndex ?? 0;
    if (idx <= 50) return "aqi-bg-good";        // $airsen-green-good
    if (idx <= 100) return "aqi-bg-moderate";   // $airsen-yellow-moderate
    if (idx <= 150) return "aqi-bg-sensitive";  // $airsen-orange-sensitive
    if (idx <= 200) return "aqi-bg-unhealthy";  // $airsen-red-unhealthy
    if (idx <= 300) return "aqi-bg-very";       // $airsen-purple-very
    return "aqi-bg-hazardous";                  // $airsen-maroon-hazardous
  }

  /**
   * Check if air quality data is available
   */
  get hasData(): boolean {
    return this.airQuality !== null && this.airQuality !== undefined;
  }

  /**
   * AQI value display with fallback
   */
  get displayAqiValue(): string {
    return this.airQuality?.globalIndex?.toString() ?? "—";
  }

  /**
   * AQI quality label (Bon, Moyen, etc.)
   */
  get aqiQualityLabel(): string {
    const idx = this.airQuality?.globalIndex ?? 0;
    if (!this.hasData) return "Inconnu";
    if (idx <= 50) return "Bon";                      // Good
    if (idx <= 100) return "Moyen";                   // Moderate
    if (idx <= 150) return "Dégradé";                 // Sensitive/Poor
    if (idx <= 200) return "Mauvais";                 // Unhealthy
    if (idx <= 300) return "Très mauvais";            // Very Unhealthy
    return "Extrêmement mauvais";                     // Hazardous
  }

  /**
   * ARIA label for screen readers with full context
   */
  get aqiAriaLabel(): string {
    const value = this.displayAqiValue;
    const label = this.aqiQualityLabel;
    return `Indice de qualité de l'air: ${value}, ${label}`;
  }

  /**
   * Data source indicator text
   * Future: This will come from backend API response with DIRECT/ESTIMATED/NOT_AVAILABLE
   */
  get dataSourceText(): string {
    if (!this.hasData) return "Non disponible";
    // Placeholder - backend will provide actual source in future
    return "Mesure directe";
  }

  /**
   * Data source icon
   */
  get dataSourceIcon(): string {
    if (!this.hasData) return "warning";
    return "sensors"; // Material icon for direct measurement
  }

  /**
   * Pollutant value getters with null handling
   */
  get hasPM25(): boolean {
    return this.airQuality?.pm25 != null;
  }

  get displayPM25(): string {
    const value = this.airQuality?.pm25;
    return value != null ? value.toString() : "N/A";
  }

  get hasPM10(): boolean {
    return this.airQuality?.pm10 != null;
  }

  get displayPM10(): string {
    const value = this.airQuality?.pm10;
    return value != null ? value.toString() : "N/A";
  }

  get hasNO2(): boolean {
    return this.airQuality?.no2 != null;
  }

  get displayNO2(): string {
    const value = this.airQuality?.no2;
    return value != null ? value.toString() : "N/A";
  }

  get hasO3(): boolean {
    return this.airQuality?.o3 != null;
  }

  get displayO3(): string {
    const value = this.airQuality?.o3;
    return value != null ? value.toString() : "N/A";
  }

  get hasSO2(): boolean {
    return this.airQuality?.so2 != null;
  }

  get displaySO2(): string {
    const value = this.airQuality?.so2;
    return value != null ? value.toString() : "N/A";
  }
}

