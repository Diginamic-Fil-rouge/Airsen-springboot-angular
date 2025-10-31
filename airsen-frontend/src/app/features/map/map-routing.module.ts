import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MapComponent } from './map.component';

/**
 * MapRoutingModule - Routing configuration for map feature
 *
 * This module defines routes for the interactive air quality map:
 * - /map → MapComponent (container with MapViewComponent, MapLegendComponent, MapDatasComponent)
 *
 * The map feature displays:
 * - Interactive geographic map with air quality indicators
 * - Real-time air quality data visualization
 * - Legend for AQI color coding
 * - Selected location data panel
 *
 * Lazy loading configuration in AppRoutingModule:
 * {
 *   path: 'map',
 *   loadChildren: () => import('./features/map/map.module').then(m => m.MapModule)
 * }
 */
const routes: Routes = [
  {
    path: '',
    component: MapComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MapRoutingModule { }
