import { Component, EventEmitter, Input, Output } from "@angular/core";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";
import { Weather } from "@/shared/models/weather.model";
import { AirQuality } from "@/shared/models/air-quality.model";

/**
 * Commune Detail Panel Component
 *
 * Displays comprehensive commune information with:
 * - Null safety checks for all data inputs
 * - Loading skeleton UI when isLoading === true
 * - Graceful degradation when data sources fail (shows "Données indisponibles")
 * - AQI color calculation using ATMO index ranges
 * - French locale formatting for numbers (population, area)
 * - Data source indicators (Direct, Estimated, Not Available)
 */
@Component({
  standalone: false,
  selector: "app-commune-detail-panel",
  templateUrl: "./commune-detail-panel.component.html",
})
export class CommuneDetailPanelComponent {
  @Input() commune: CommuneWithAirQuality | null = null;
  @Input() weatherData: Weather | null = null;
  @Input() airQualityData: AirQuality | null = null;
  @Input() isLoading: boolean = false;

  @Output() close = new EventEmitter<void>();
  @Output() addToFavorites = new EventEmitter<void>();
  @Output() exportPDF = new EventEmitter<void>();

  /**
   * Event handlers for user actions
   */
  onClose(): void {
    this.close.emit();
  }

  onAddToFavorites(): void {
    this.addToFavorites.emit();
  }

  onExportPDF(): void {
    this.exportPDF.emit();
  }

  /**
   * AQI Value - prioritizes commune data, falls back to airQualityData
   */
  get aqiValue(): number | null {
    if (this.commune?.currentAirQuality?.atmoIndex != null) {
      return this.commune.currentAirQuality.atmoIndex;
    }
    if (this.airQualityData?.globalIndex != null) {
      return this.airQualityData.globalIndex;
    }
    return null;
  }

  /**
   * AQI Label - French quality descriptor (Bon, Moyen, Dégradé, etc.)
   */
  get aqiLabel(): string {
    return this.commune?.currentAirQuality?.qualifier || this.airQualityData?.globalQuality || "Inconnu";
  }

  /**
   * AQI Color - Hex color for visual indicator
   */
  get aqiColor(): string {
    return this.commune?.currentAirQuality?.color || "#999999";
  }

  /**
   * AQI Background Class - Returns design-system CSS class based on ATMO index
   * Follows AIRSEN design-system.scss AQI color palette
   */
  get aqiBgClass(): string {
    const index = this.aqiValue;
    if (index === null) return "aqi-bg-unknown";

    if (index <= 50) return "aqi-bg-good";        // $airsen-green-good
    if (index <= 100) return "aqi-bg-moderate";   // $airsen-yellow-moderate
    if (index <= 150) return "aqi-bg-sensitive";  // $airsen-orange-sensitive
    if (index <= 200) return "aqi-bg-unhealthy";  // $airsen-red-unhealthy
    if (index <= 300) return "aqi-bg-very";       // $airsen-purple-very
    return "aqi-bg-hazardous";                    // $airsen-maroon-hazardous
  }

  /**
   * Check if commune data is available (not null)
   */
  get hasCommuneData(): boolean {
    return this.commune !== null && this.commune !== undefined;
  }

  /**
   * Check if weather data is available
   */
  get hasWeatherData(): boolean {
    return this.weatherData !== null && this.weatherData !== undefined;
  }

  /**
   * Check if air quality data is available
   */
  get hasAirQualityData(): boolean {
    return this.aqiValue !== null;
  }

  /**
   * Format population with French locale (e.g., 1234567 → "1 234 567")
   */
  get formattedPopulation(): string {
    const population = this.commune?.population;
    if (population == null) return "—";
    return new Intl.NumberFormat("fr-FR").format(population);
  }

  /**
   * Calculate and format population density
   * Note: Area is not in current Commune model, placeholder for future implementation
   */
  get formattedDensity(): string {
    // Placeholder - area field doesn't exist in current Commune model
    // Future: When area is added to backend, calculate: population / area
    return "—";
  }

  /**
   * Get commune name with null safety
   */
  get communeName(): string {
    return this.commune?.name || "—";
  }

  /**
   * Get department code with null safety
   */
  get departmentCode(): string {
    return this.commune?.departmentCode || "—";
  }

  /**
   * Get department name with null safety
   */
  get departmentName(): string {
    return this.commune?.department?.name || this.departmentCode;
  }

  /**
   * Get region name with null safety
   */
  get regionName(): string {
    return this.commune?.department?.region?.name || "—";
  }

  /**
   * Temperature display with null safety
   */
  get temperature(): number | null {
    return this.weatherData?.temperature ?? null;
  }

  /**
   * Humidity display with null safety
   */
  get humidity(): number | null {
    return this.weatherData?.humidity ?? null;
  }

  /**
   * Wind speed display with null safety
   */
  get windSpeed(): number | null {
    return this.weatherData?.windSpeed ?? null;
  }

  /**
   * Weather code with null safety
   */
  get weatherCode(): number | null {
    return this.weatherData?.weatherCode ?? null;
  }

  /**
   * Data source indicator for air quality
   * Returns French labels for data transparency
   */
  get airQualityDataSource(): string {
    // This would come from backend API response in future
    // For now, return default based on data availability
    if (!this.hasAirQualityData) return "Non disponible";
    return "Mesure directe"; // Default - backend will provide actual source
  }

  /**
   * Data source indicator for weather
   */
  get weatherDataSource(): string {
    // This would come from backend API response in future
    if (!this.hasWeatherData) return "Non disponible";
    return "Mesure directe"; // Default - backend will provide actual source
  }

  /**
   * Check if component should show loading skeleton
   */
  get showLoadingSkeleton(): boolean {
    return this.isLoading && !this.hasCommuneData;
  }

  /**
   * Check if component should show content
   */
  get showContent(): boolean {
    return !this.isLoading && this.hasCommuneData;
  }

  /**
   * Check if component should show error state (no data and not loading)
   */
  get showErrorState(): boolean {
    return !this.isLoading && !this.hasCommuneData;
  }
}

