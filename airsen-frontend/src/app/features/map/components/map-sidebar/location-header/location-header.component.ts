import { Component } from '@angular/core';

/**
 * LocationHeaderComponent
 *
 * Purpose: Displays the currently selected location (commune name, INSEE) and
 * high-level context like department or region for quick orientation.
 */
@Component({
  standalone: false,
  selector: 'app-location-header',
  templateUrl: './location-header.component.html',
  styleUrls: ['./location-header.component.scss']
})
export class LocationHeaderComponent {}

