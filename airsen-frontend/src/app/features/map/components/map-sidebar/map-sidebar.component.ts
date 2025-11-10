import { Component, Input } from '@angular/core';

/**
 * MapSidebarComponent
 *
 * Purpose: Container for the map's left sidebar that hosts the search bar,
 * location header, AQI hero display, health recommendations, pollutant breakdown,
 * trend chart, and weather summary. This component organizes layout and delegates
 * actual rendering to its child components.
 */
@Component({
  standalone: false,
  selector: 'app-map-sidebar',
  templateUrl: './map-sidebar.component.html',
  styleUrls: ['./map-sidebar.component.scss']
})
export class MapSidebarComponent {
  /** When true, shows loading placeholders for child widgets */
  @Input() isLoading = false;
}

