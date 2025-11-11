import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { MapComponent } from "./map.component";

/**
 * Map Routes
 *
 * Purpose: Configure routing for map feature module
 * Parent path: /map (from AppRoutingModule)
 * Local paths: ''
 *
 * Routes:
 * - '' → MapComponent (default route when navigating to /map)
 *
 * Features:
 * - Interactive map with air quality data visualization
 * - Color-coded markers representing ATMO index for each commune
 * - Real-time data fetched from AIRSEN backend API
 * - Marker clustering for improved performance with 35K+ communes
 * - Commune information popups with AQI details
 *
 * Security:
 * - Public access (no authentication required)
 * - Air quality data is public information
 * - Uses cached backend data to minimize external API calls
 *
 * Performance Considerations:
 * - Lazy-loaded module reduces initial bundle size
 * - Map tiles and markers loaded progressively
 * - Clustering prevents DOM overload with thousands of markers
 * - Backend caching minimizes ATMO France API requests
 *
 * Learning Point:
 * Map features require careful performance optimization due to:
 * 1. Large datasets (35K+ communes with coordinates)
 * 2. DOM manipulation for thousands of markers
 * 3. Third-party library integration (Leaflet)
 * 4. Real-time data updates from external APIs
 */
const routes: Routes = [
  {
    path: "",
    component: MapComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class MapRoutingModule {}
