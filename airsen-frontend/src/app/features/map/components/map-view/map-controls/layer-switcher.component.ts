import { Component, Output, EventEmitter } from '@angular/core';
import { MapStyle } from '../../../models/map-filter.model';

@Component({
  standalone: false,
  selector: 'app-layer-switcher',
  templateUrl: './layer-switcher.component.html',
  styleUrls: ['./layer-switcher.component.scss']
})
export class LayerSwitcherComponent {
  @Output() layerChanged = new EventEmitter<MapStyle>();

  selectedLayer: MapStyle = MapStyle.STREETS;
  isExpanded: boolean = false;

  layers = [
    { value: MapStyle.STREETS, label: 'Carte', icon: 'map' },
    { value: MapStyle.SATELLITE, label: 'Satellite', icon: 'satellite_alt' },
    { value: MapStyle.TERRAIN, label: 'Relief', icon: 'terrain' },
    { value: MapStyle.DARK, label: 'Sombre', icon: 'dark_mode' }
  ];

  toggleExpand(): void {
    this.isExpanded = !this.isExpanded;
  }

  selectLayer(layer: MapStyle): void {
    this.selectedLayer = layer;
    this.layerChanged.emit(layer);
    this.isExpanded = false;
  }

  get selectedLayerInfo() {
    return this.layers.find(l => l.value === this.selectedLayer);
  }
}
