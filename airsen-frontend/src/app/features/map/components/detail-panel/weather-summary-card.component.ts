import { Component, Input } from "@angular/core";
import { Weather } from "@/shared/models/weather.model";

/**
 * Weather Summary Card Component
 *
 * Displays current weather conditions with:
 * - Weather icon mapping (WMO weather code → Material icon)
 * - Temperature with unit conversion support
 * - Null handling for optional fields (wind, humidity, measurement date)
 * - French locale date/time formatting
 * - Accessibility: ARIA labels for weather conditions
 */
@Component({
  standalone: false,
  selector: "app-weather-summary-card",
  template: `
    <div class="weather-summary-card" role="region" aria-label="Météo actuelle">
      <!-- Primary Weather Display -->
      <div class="primary" *ngIf="hasData">
        <div class="weather-icon-container">
          <mat-icon class="weather-icon" [attr.aria-label]="weatherDescription">{{ weatherIcon }}</mat-icon>
        </div>
        <div class="temp-container">
          <div class="temp" [attr.aria-label]="'Température: ' + displayTemperature">{{ displayTemperature }}</div>
          <div class="desc" aria-hidden="true">{{ weatherDescription }}</div>
        </div>
      </div>

      <!-- Secondary Weather Details -->
      <div class="secondary" *ngIf="hasData">
        <div class="item" *ngIf="hasWindSpeed">
          <mat-icon class="detail-icon">air</mat-icon>
          <span class="detail-label">Vent</span>
          <strong class="detail-value" [attr.aria-label]="'Vitesse du vent: ' + displayWindSpeed">{{ displayWindSpeed }}</strong>
        </div>
        <div class="item" *ngIf="hasHumidity">
          <mat-icon class="detail-icon">water_drop</mat-icon>
          <span class="detail-label">Humidité</span>
          <strong class="detail-value" [attr.aria-label]="'Humidité: ' + displayHumidity">{{ displayHumidity }}</strong>
        </div>
        <div class="item" *ngIf="hasWindDirection">
          <mat-icon class="detail-icon">navigation</mat-icon>
          <span class="detail-label">Direction</span>
          <strong class="detail-value" [attr.aria-label]="'Direction du vent: ' + displayWindDirection">{{ displayWindDirection }}</strong>
        </div>
      </div>

      <!-- Measurement Time -->
      <div class="measurement-time" *ngIf="hasData && hasMeasurementDate">
        <mat-icon class="time-icon">schedule</mat-icon>
        <span class="time-text">{{ formattedMeasurementDate }}</span>
      </div>

      <!-- No Data State -->
      <div class="no-data" *ngIf="!hasData">
        <mat-icon>cloud_off</mat-icon>
        <p>Données météo indisponibles</p>
      </div>
    </div>
  `,
})
export class WeatherSummaryCardComponent {
  @Input() weather: Weather | null = null;

  /**
   * Check if weather data is available
   */
  get hasData(): boolean {
    return this.weather !== null && this.weather !== undefined;
  }

  /**
   * Temperature display with unit (always Celsius for French locale)
   */
  get displayTemperature(): string {
    const temp = this.weather?.temperature;
    if (temp == null) return "—";
    return `${Math.round(temp)}°C`;
  }

  /**
   * Wind speed display with unit
   */
  get hasWindSpeed(): boolean {
    return this.weather?.windSpeed != null;
  }

  get displayWindSpeed(): string {
    const speed = this.weather?.windSpeed;
    if (speed == null) return "—";
    return `${Math.round(speed)} km/h`;
  }

  /**
   * Humidity display with percentage
   */
  get hasHumidity(): boolean {
    return this.weather?.humidity != null;
  }

  get displayHumidity(): string {
    const humidity = this.weather?.humidity;
    if (humidity == null) return "—";
    return `${Math.round(humidity)}%`;
  }

  /**
   * Wind direction display (converts degrees to cardinal directions)
   */
  get hasWindDirection(): boolean {
    return this.weather?.windDirection != null;
  }

  get displayWindDirection(): string {
    const degrees = this.weather?.windDirection;
    if (degrees == null) return "—";

    // Convert degrees to cardinal directions (French)
    const directions = ["N", "NE", "E", "SE", "S", "SO", "O", "NO"];
    const index = Math.round(((degrees % 360) / 45)) % 8;
    return directions[index];
  }

  /**
   * Measurement date check
   */
  get hasMeasurementDate(): boolean {
    return this.weather?.measurementDate != null;
  }

  /**
   * Format measurement date with French locale
   * Example: "Mis à jour le 10 nov. 2025 à 14:30"
   */
  get formattedMeasurementDate(): string {
    const date = this.weather?.measurementDate;
    if (!date) return "";

    const dateObj = new Date(date);
    const now = new Date();
    const diffMinutes = Math.floor((now.getTime() - dateObj.getTime()) / 60000);

    // Show relative time if recent (< 60 minutes)
    if (diffMinutes < 60) {
      if (diffMinutes < 1) return "À l'instant";
      if (diffMinutes === 1) return "Il y a 1 minute";
      return `Il y a ${diffMinutes} minutes`;
    }

    // Otherwise show formatted date/time
    return new Intl.DateTimeFormat("fr-FR", {
      day: "numeric",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    }).format(dateObj);
  }

  /**
   * Weather icon mapping based on WMO weather codes
   * Maps weatherCode to Material Design icon names
   */
  get weatherIcon(): string {
    const code = this.weather?.weatherCode;
    if (code == null) return "help";

    // WMO Weather Code to Material Icon mapping
    if (code === 0) return "wb_sunny";                    // Clear sky
    if (code >= 1 && code <= 3) return "wb_cloudy";       // Partly cloudy to overcast
    if (code >= 45 && code <= 48) return "foggy";         // Fog
    if (code >= 51 && code <= 55) return "grain";         // Drizzle
    if (code >= 61 && code <= 65) return "water_drop";    // Rain
    if (code >= 71 && code <= 75) return "ac_unit";       // Snow
    if (code >= 80 && code <= 82) return "shower";        // Rain showers
    if (code >= 95 && code <= 99) return "thunderstorm";  // Thunderstorm

    return "cloud"; // Default fallback
  }

  /**
   * Weather description in French based on WMO weather codes
   */
  get weatherDescription(): string {
    const code = this.weather?.weatherCode;
    if (code == null) return "Inconnu";

    // WMO Weather Code to French description mapping
    if (code === 0) return "Ciel dégagé";
    if (code === 1) return "Principalement dégagé";
    if (code === 2) return "Partiellement nuageux";
    if (code === 3) return "Couvert";
    if (code === 45 || code === 48) return "Brouillard";
    if (code === 51) return "Bruine légère";
    if (code === 53) return "Bruine modérée";
    if (code === 55) return "Bruine dense";
    if (code === 61) return "Pluie légère";
    if (code === 63) return "Pluie modérée";
    if (code === 65) return "Pluie forte";
    if (code === 71) return "Neige légère";
    if (code === 73) return "Neige modérée";
    if (code === 75) return "Neige forte";
    if (code === 80) return "Averses légères";
    if (code === 81) return "Averses modérées";
    if (code === 82) return "Averses fortes";
    if (code === 95) return "Orage";
    if (code === 96) return "Orage avec grêle légère";
    if (code === 99) return "Orage avec grêle forte";

    return `Code météo: ${code}`;
  }
}

