import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MapRoutingModule } from "./map-routing.module";
import { SharedModule } from "@/shared/shared.module";

// Components
import { MapComponent } from "./map.component";
import { LeafletMapComponent } from "./components/leaflet-map/leaflet-map.component";

/**
 * Map Feature Module
 *
 * Purpose: Interactive map displaying air quality data across French communes
 * Features:
 * - Leaflet-based map with OpenStreetMap tiles
 * - Color-coded AQI markers (ATMO France standard)
 * - Marker clustering for performance
 * - Commune information popups
 * - Real-time air quality data from backend
 *
 * Components:
 * - MapComponent: Container component managing data flow
 * - LeafletMapComponent: Core map rendering and interaction logic
 *
 * Dependencies:
 * - leaflet: Core mapping library
 * - leaflet.markercluster: Marker clustering functionality
 * - SharedModule: Common AIRSEN components and services
 *
 * Architecture Integration:
 * Uses AIRSEN's established patterns:
 * - Component-service separation
 * - Observable data streams
 * - Material Design components
 * - Shared module for common functionality
 */
@NgModule({
  declarations: [MapComponent, LeafletMapComponent],
  imports: [CommonModule, MapRoutingModule, SharedModule],
})
export class MapModule {}
