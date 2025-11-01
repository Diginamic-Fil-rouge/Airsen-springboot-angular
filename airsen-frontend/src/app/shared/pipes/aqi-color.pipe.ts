import { Pipe, PipeTransform } from '@angular/core';

/**
 * AqiColorPipe - Convert AQI numeric value to corresponding hex color
 *
 * Transforms Air Quality Index (AQI) numbers into color codes
 * following EPA AQI color standards for visual consistency:
 * - 0-50: Green (#00E400) - Good
 * - 51-100: Yellow (#FFFF00) - Moderate
 * - 101-150: Orange (#FF7E00) - Unhealthy for Sensitive Groups
 * - 151-200: Red (#FF0000) - Unhealthy
 * - 201-300: Purple (#8F3F97) - Very Unhealthy
 * - 301+: Maroon (#7E0023) - Hazardous
 *
 * Usage in template:
 * <div [style.background-color]="aqiValue | aqiColor">...</div>
 *
 * Example:
 * {{ 75 | aqiColor }}  // Output: "#FFFF00"
 * {{ 180 | aqiColor }} // Output: "#FF0000"
 */
@Pipe({
  name: 'aqiColor',
  standalone: false
})
export class AqiColorPipe implements PipeTransform {
  /**
   * Transform AQI number to hex color code
   *
   * @param value - AQI numeric value (0-500+)
   * @returns Hex color code for AQI category
   */
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '#CCCCCC';
    }

    if (value <= 50) {
      return '#00E400'; // Green - Good
    } else if (value <= 100) {
      return '#FFFF00'; // Yellow - Moderate
    } else if (value <= 150) {
      return '#FF7E00'; // Orange - Unhealthy for Sensitive Groups
    } else if (value <= 200) {
      return '#FF0000'; // Red - Unhealthy
    } else if (value <= 300) {
      return '#8F3F97'; // Purple - Very Unhealthy
    } else {
      return '#7E0023'; // Maroon - Hazardous
    }
  }
}
