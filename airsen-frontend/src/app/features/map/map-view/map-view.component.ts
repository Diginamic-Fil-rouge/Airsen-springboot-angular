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

  // communes$ = this.geographicService.getCommunesWithCoordinates();
  communes = input<Observable<Commune[]>>();
  @Output() onMarkerClick = new EventEmitter<any>();

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnChanges() {
    this.initMarkers();
  }

  private initMap(): void {
    this.map = L.map('map', {
      // coordinates of the middle of France
      center: [46.3622, 1.5231],
      zoom: 6
    });

    const tiles = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      minZoom: 3,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    });

    tiles.addTo(this.map);
  }

  initMarkers(){
    this.communes()?.forEach(communes => {
      console.log("commune : ", communes);
      communes.forEach(commune => {
        this.addMarketToMap(commune);
      });
    });
  }

  addMarketToMap(commune: any) {
    if (!commune.latitude || !commune.longitude) {
      return;
    }
    L.marker([commune.latitude, commune.longitude]).addTo(this.map).on('click', () => {
      this.onMarkerClick.emit(commune);
    });
  }

}
