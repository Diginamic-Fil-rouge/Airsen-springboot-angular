import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// Material imports
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';

// Routing
import { MapRoutingModule } from './map-routing.module';

// Components
import { MapComponent } from './map.component';
import { MapViewComponent } from './components/map-view/map-view.component';
import { FilterControlsComponent } from './components/side-panel/filter-controls.component';
import { StationListComponent } from './components/side-panel/station-list.component';
import { LayerSwitcherComponent } from './components/map-view/map-controls/layer-switcher.component';
import { StationDetailCardComponent } from './components/station-details/station-detail-card.component';

// Services
import { MapService } from './services/map.service';

@NgModule({
  declarations: [
    MapComponent,
    MapViewComponent,
    FilterControlsComponent,
    StationListComponent,
    LayerSwitcherComponent,
    StationDetailCardComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MapRoutingModule,
    // Material
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatCardModule
  ],
  providers: [
    MapService
  ]
})
export class MapModule { }
