import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { SharedModule } from "@/shared/shared.module";
import { DashboardModule } from "@/features/dashboard/dashboard.module";

import { MapRoutingModule } from "./map-routing.module";
import { MapComponent } from "./map.component";
import { MapViewComponent } from "./map-view/map-view.component";
import { MapLegendComponent } from "./map-view/legend/map-legend.component";
import { MapDataComponent } from "./data/map-data.component";

/**
 * MapModule - Lazy-loaded map feature module
 *
 * This module encapsulates the interactive air quality map feature.
 * It is lazy-loaded to reduce initial bundle size and improve performance.
 */
@NgModule({
  declarations: [MapComponent, MapViewComponent, MapLegendComponent, MapDataComponent],
  imports: [CommonModule, SharedModule, MapRoutingModule, DashboardModule],
})
export class MapModule {}
