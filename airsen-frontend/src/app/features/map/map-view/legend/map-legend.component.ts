import { Component } from '@angular/core';

@Component({
  standalone: false,
  selector: 'app-map-legend',
  templateUrl: './map-legend.component.html',
  styleUrls: ['./map-legend.component.scss']
})
export class MapLegendComponent {
  displayLegend: boolean = false;

  toggleLegend(){
    this.displayLegend = !this.displayLegend;
  }
}

