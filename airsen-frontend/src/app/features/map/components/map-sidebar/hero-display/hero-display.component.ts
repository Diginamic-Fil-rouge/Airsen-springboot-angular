import { ChangeDetectionStrategy, Component, Input } from "@angular/core";

/**
 * Hero Display Component
 *
 * Purpose: Displays the primary Air Quality Index (ATMO index) as a large,
 * color-coded number with badge label and animated mascot character.
 *
 * Features:
 * - Large circular AQI display (120px) with background color from backend
 * - Animated emoji mascot reflecting air quality status (😊 to ☠️)
 * - Material chip badge showing qualifier ("Bon", "Moyen", etc.)
 * - Fully accessible with ARIA labels and semantic HTML
 * - OnPush change detection for performance
 *
 * Integration:
 * Used in MapSidebarComponent to show hero AQI when commune is selected.
 * Receives data from parent via @Input bindings.
 *
 * Architecture:
 * Pure presentational component - no API calls, no internal state.
 * All data flows in via inputs from parent component.
 */
@Component({
  standalone: false,
  selector: "app-hero-display",
  templateUrl: "./hero-display.component.html",
  styleUrls: ["./hero-display.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeroDisplayComponent {
  /**
   * ATMO index value (1-6 scale for ATMO France standard)
   * 1: Bon, 2: Moyen, 3: Dégradé, 4: Mauvais, 5: Très mauvais, 6: Extrêmement mauvais
   */
  @Input() atmoIndex: number | null = null;

  /**
   * Text qualifier for air quality level
   * Examples: "Bon", "Moyen", "Dégradé", "Mauvais", "Très mauvais", "Extrêmement mauvais"
   */
  @Input() qualifier: string | null = null;

  /**
   * Hex color code from backend corresponding to ATMO level
   * Examples: #50F0E6 (Bon), #50CCAA (Moyen), #F0E641 (Dégradé)
   */
  @Input() color: string | null = null;

  /**
   * Whether to show animated mascot emoji (default: true)
   */
  @Input() showMascot = true;

  /**
   * Computed display value for AQI circle
   * Returns '?' if atmoIndex is null, otherwise the number as string
   */
  get displayValue(): string {
    return this.atmoIndex !== null ? this.atmoIndex.toString() : "?";
  }

  /**
   * Computed mascot emoji based on ATMO index
   * Maps air quality levels to expressive emojis for emotional connection
   */
  get mascotEmoji(): string {
    if (!this.atmoIndex) return "😐";

    const emojiMap: Record<number, string> = {
      1: "😊", // Bon (Good)
      2: "🙂", // Moyen (Moderate)
      3: "😐", // Dégradé (Unhealthy for Sensitive)
      4: "😷", // Mauvais (Unhealthy)
      5: "🤢", // Très mauvais (Very Unhealthy)
      6: "☠️", // Extrêmement mauvais (Hazardous)
    };

    return emojiMap[this.atmoIndex] || "😐";
  }

  /**
   * ARIA label for screen readers
   * Provides accessible description of air quality status
   */
  get ariaLabel(): string {
    const qualifierText = this.qualifier || "Inconnu";
    return `Indice ATMO ${this.displayValue}, qualité de l'air ${qualifierText}`;
  }

  /**
   * CSS filter effect for mascot based on air quality
   * Better quality = brighter, worse quality = darker/desaturated
   */
  getFilterForQuality(): string {
    if (!this.atmoIndex) return "grayscale(100%)";

    if (this.atmoIndex <= 2) return "brightness(1.1)"; // Happy/bright
    if (this.atmoIndex <= 4) return "saturate(0.8)"; // Neutral
    return "contrast(1.2) brightness(0.9)"; // Unhealthy/dark
  }

  /**
   * Calculates optimal text color (black or white) for given background
   * Uses luminance calculation to ensure WCAG AA contrast ratio
   */
  getContrastColor(bgColor: string | null): string {
    if (!bgColor) return "#FFFFFF";

    const hex = bgColor.replace("#", "");
    const r = parseInt(hex.substr(0, 2), 16);
    const g = parseInt(hex.substr(2, 2), 16);
    const b = parseInt(hex.substr(4, 2), 16);

    // Calculate relative luminance
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;

    // Return black for light backgrounds, white for dark backgrounds
    return luminance > 0.5 ? "#000000" : "#FFFFFF";
  }
}
