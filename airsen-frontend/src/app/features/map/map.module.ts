import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";

// Material imports
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatAutocompleteModule } from "@angular/material/autocomplete";

// Routing
import { MapRoutingModule } from "./map-routing.module";

// Components
import { MapComponent } from "./map.component";
import { MapViewComponent } from "./components/map-view/map-view.component";
import { LayerSwitcherComponent } from "./components/map-view/map-controls/layer-switcher.component";
import { CommuneDetailCardComponent } from "./components/commune-details/commune-detail-card.component";
import { MapSearchBarComponent } from "./components/search/map-search-bar.component";
import { CommuneDetailPanelComponent } from "./components/detail-panel/commune-detail-panel.component";
import { AqiSummaryCardComponent } from "./components/detail-panel/aqi-summary-card.component";
import { WeatherSummaryCardComponent } from "./components/detail-panel/weather-summary-card.component";
// New scaffolding components (Task 1.3)
import { MapSidebarComponent } from "./components/map-sidebar/map-sidebar.component";
import { SearchBarComponent } from "./components/map-sidebar/search-bar/search-bar.component";
import { LocationHeaderComponent } from "./components/map-sidebar/location-header/location-header.component";
import { HeroDisplayComponent } from "./components/map-sidebar/hero-display/hero-display.component";
import { HealthRecommendationsComponent } from "./components/map-sidebar/health-recommendations/health-recommendations.component";
import { PollutantBreakdownComponent } from "./components/map-sidebar/pollutant-breakdown/pollutant-breakdown.component";
import { TrendChartComponent } from "./components/map-sidebar/trend-chart/trend-chart.component";
import { WeatherSummaryComponent } from "./components/map-sidebar/weather-summary/weather-summary.component";
import { LeafletMapComponent } from "./components/leaflet-map/leaflet-map.component";
import { MapControlsComponent } from "./components/leaflet-map/map-controls/map-controls.component";
import { MapLegendComponent } from "./components/map-legend/map-legend.component";

// Services
import { MapService } from "./services/map.service";

@NgModule({
  declarations: [
    MapComponent,
    MapViewComponent,
    LayerSwitcherComponent,
    CommuneDetailCardComponent,
    MapSearchBarComponent,
    CommuneDetailPanelComponent,
    AqiSummaryCardComponent,
    WeatherSummaryCardComponent,
    // New scaffolded components
    MapSidebarComponent,
    SearchBarComponent,
    LocationHeaderComponent,
    HeroDisplayComponent,
    HealthRecommendationsComponent,
    PollutantBreakdownComponent,
    TrendChartComponent,
    WeatherSummaryComponent,
    LeafletMapComponent,
    MapControlsComponent,
    MapLegendComponent,
  ],
  imports: [
    CommonModule,
    MapRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    // Material
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
  ],
  providers: [MapService],
})
export class MapModule {}
