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
