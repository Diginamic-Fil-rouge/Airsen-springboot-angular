import { Component } from '@angular/core';

/**
 * PollutantBreakdownComponent
 *
 * Purpose: Shows a compact breakdown of key pollutants (PM2.5, PM10, NO2, O3)
 * with values and simple bars/indicators to visualize levels.
 */
@Component({
  standalone: false,
  selector: 'app-pollutant-breakdown',
  templateUrl: './pollutant-breakdown.component.html',
  styleUrls: ['./pollutant-breakdown.component.scss']
})
export class PollutantBreakdownComponent {}

