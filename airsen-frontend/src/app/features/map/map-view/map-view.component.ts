import { Component, AfterViewInit, input, inject, Output, EventEmitter } from '@angular/core';
import * as L from 'leaflet';
import { Observable } from 'rxjs';
import { Commune } from '../models/commune.model';
import { GeographicService } from '../services/geographic.service';

@Component({
  standalone: false,
  selector: 'app-map-view',
  templateUrl: './map-view.component.html',
  styleUrls: ['./map-view.component.scss']
})
export class MapViewComponent implements AfterViewInit {
  private map: any;
  private geographicService = inject(GeographicService)

  markersInitialized = false;
  communes = input<Observable<Commune[]>>();
  communeClicked = input<Commune | null>();
  markers: L.Marker[] = [];
  @Output() onMarkerClick = new EventEmitter<any>();

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnChanges() {
    this.initMarkers();
  }

  private initMap(): void {
    this.map = L.map('map', {
      // coordinates of France
      center: [47.0, 1.5231],
      zoom: 7
    });

    const tiles = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      minZoom: 3,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    });

    tiles.addTo(this.map);
  }

  initMarkers(){
    if (!this.markersInitialized){
      this.communes()?.forEach(communes => {
        console.log("commune : ", communes);
        communes.forEach(commune => {
          this.addMarketToMap(commune);
        });
      });
      this.markersInitialized = true;
    }

    if (this.communeClicked()){
      this.addMarketToMap(this.communeClicked());
    }
  }

  addMarketToMap(commune: any) {
    if (!commune.latitude || !commune.longitude) {
      return;
    }
    const icon = L.icon({
      iconUrl: 'assets/images/marker.png',
      iconSize: [20, 20],
      iconAnchor: [10, 5],
      popupAnchor: [-3, -76]
    });
    
    this.markers.push(L.marker([commune.latitude, commune.longitude], { icon: icon }).addTo(this.map).on('click', () => {
      this.onMarkerClick.emit(commune);
    }));
  }

}
