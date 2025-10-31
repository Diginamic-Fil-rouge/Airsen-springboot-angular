import { Pipe, PipeTransform } from '@angular/core';

/**
 * AqiLabelPipe - Convert AQI numeric value to human-readable label
 *
 * Transforms Air Quality Index (AQI) numbers into descriptive labels
 * following EPA AQI standards:
 * - 0-50: Good
 * - 51-100: Moderate
 * - 101-150: Unhealthy for Sensitive Groups
 * - 151-200: Unhealthy
 * - 201-300: Very Unhealthy
 * - 301+: Hazardous
 *
 * Usage in template:
 * {{ aqiValue | aqiLabel }}
 *
 * Example:
 * {{ 75 | aqiLabel }}  // Output: "Moderate"
 * {{ 180 | aqiLabel }} // Output: "Unhealthy"
 */
@Pipe({
  name: 'aqiLabel',
  standalone: false
})
export class AqiLabelPipe implements PipeTransform {
  /**
   * Transform AQI number to label
   *
   * @param value - AQI numeric value (0-500+)
   * @returns Human-readable AQI category label
   */
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return 'N/A';
    }

    if (value <= 50) {
      return 'Good';
    } else if (value <= 100) {
      return 'Moderate';
    } else if (value <= 150) {
      return 'Unhealthy for Sensitive Groups';
    } else if (value <= 200) {
      return 'Unhealthy';
    } else if (value <= 300) {
      return 'Very Unhealthy';
    } else {
      return 'Hazardous';
    }
  }
}
