import { Component, AfterViewInit, input, inject, Output, EventEmitter, ViewContainerRef, Injector, ComponentRef, createComponent } from '@angular/core';
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
  communeSearched = input<Commune | null>();
  @Output() onMarkerClick = new EventEmitter<any>();

  constructor(
    private viewContainerRef: ViewContainerRef,
    private injector: Injector
  ) { }

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnChanges() {
    this.initMarkers();
  }

/**
 * Initializes the Leaflet map with France's coordinates
 * and adds a tile layer to display OpenStreetMap tiles.
 * The map is centered on France with a zoom level of 7.
 * The tile layer is configured to display tiles from OpenStreetMap
 * with a maximum zoom level of 18 and a minimum zoom level of 3.
 * The attribution for the tile layer is set to the OpenStreetMap
 * copyright notice.
 */
  private initMap(): void {
    this.map = L.map('map', {
      // coordinates of France
      center: [47.0, 1.5231],
      zoom: 6
    });

    const tiles = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      minZoom: 3,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    });

    tiles.addTo(this.map);
  }

  /**
   * Initializes the markers on the map.
   * If the markers have not been initialized yet, it fetches the communes from the input observable and adds a marker to the map for each commune.
   * If a commune has been clicked, it fetches the commune data from the geographic service and adds a marker to the map for the commune.
   * The markers are only initialized once, and then the function does nothing.
   */
  initMarkers() {
    if (!this.markersInitialized) {
      this.communes()?.forEach(communes => {
        console.log("commune : ", communes);
        communes.forEach(commune => {
          this.addMarkerToMap(commune);
        });
      });
      this.markersInitialized = true;
    }

    if (this.communeSearched()) {
      let communeData: Commune = {
        id: 0,
        inseeCode: '',
        name: '',
        departmentCode: '',
        regionCode: '',
        population: 0,
        latitude: 0,
        longitude: 0,
        qualifier: '',
        color: '',
        atmoIndex: 0
      };
      this.geographicService.getCommuneDatas(this.communeSearched()?.inseeCode).subscribe({
        next: (data) => {
          communeData.inseeCode = data.commune.inseeCode;
          communeData.name = data.commune.name;
          communeData.departmentCode = data.commune.department.departmentCode;
          communeData.regionCode = data.commune.department.region.regionCode;
          communeData.population = data.commune.population;
          communeData.latitude = data.commune.latitude;
          communeData.longitude = data.commune.longitude;
          communeData.qualifier = data.airQuality?.atmoQual;
          communeData.color = data.airQuality?.atmoColor;
          communeData.atmoIndex = data.airQuality?.atmIndex;
          console.log("communeData : ", communeData);
          this.addMarkerToMap(communeData);
          this.zoomOnMap([communeData.latitude, communeData.longitude], 11);
        },
        error: (error) => {
          console.error('Error fetching commune data:', error);
        }
      });
    }
  }

  /**
   * Adds a marker to the map for the given commune.
   * The marker is displayed at the commune's latitude and longitude,
   * and its icon is determined by the commune's atmoIndex.
   * When the marker is clicked, emits the onMarkerClick event with the commune,
   * and opens a popup displaying the commune's data.
   * @param commune The commune data to add to the map.
   */
  addMarkerToMap(commune: any) {
    if (!commune.latitude || !commune.longitude) {
      return;
    }

    let icon: any = L.icon({
    iconUrl: `assets/images/marker-${commune.atmoIndex}.png`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
    popupAnchor: [-3, -76]
  });

    const marker = L.marker([commune.latitude, commune.longitude], { icon: icon }).addTo(this.map);

    marker.on('click', () => {
      this.onMarkerClick.emit(commune);
    });
  }

/**
 * Zooms the map to the given latitude and longitude at the given zoom level.
 * @param {any} latLng - The latitude and longitude to zoom to.
 * @param {number} zoomLevel - The zoom level to use.
 */
zoomOnMap(latLng: any, zoomLevel: number){
  this.map.setView(latLng, zoomLevel);
}

}
