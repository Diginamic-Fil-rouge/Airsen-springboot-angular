import { Component, AfterViewInit, input, inject, Output, EventEmitter, ViewContainerRef, Injector, ComponentRef, createComponent } from '@angular/core';
import * as L from 'leaflet';
import { Observable } from 'rxjs';
import { Commune, CommuneWithAirQuality } from '@/core/models';
import { GeographicService } from '../services/geographic.service';
import { FavoritesService } from '../../favorites/services/favorites.service';
import { AuthService } from '../../../core/auth/services/auth.service';
import { AuthUser } from '@/app/core/auth/models/auth.model';

@Component({
  standalone: false,
  selector: 'app-map-view',
  templateUrl: './map-view.component.html',
  styleUrls: ['./map-view.component.scss']
})
export class MapViewComponent implements AfterViewInit {
  private map: any;
  private geographicService = inject(GeographicService);
  private favoriteService = inject(FavoritesService);
  private authService = inject(AuthService);
  markersInitialized = false;
  communes = input<Observable<Commune[]>>();
  favoriteCommunes: Commune[] | null = null;
  communeSearched = input<Commune | null>();
  @Output() onMarkerClick = new EventEmitter<any>();

  constructor(
    private viewContainerRef: ViewContainerRef,
    private injector: Injector
  ) { }

  ngAfterViewInit(): void {
    this.initMap();
    this.initFavorites();
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
          this.addMarkerToMap(commune, null);
        });
      });
      this.markersInitialized = true;
    }

    if (this.communeSearched()) {
      this.geographicService.getCommuneExportDatas(this.communeSearched()?.inseeCode).subscribe({
        next: (data) => {
          const communeData: CommuneWithAirQuality = {
            id: 0,
            inseeCode: data.inseeCode,
            name: data.commune,
            population: 0,
            latitude: 0,
            longitude: 0,
            department: {
              id: 0,
              code: '',
              name: ''
            },
            currentAirQuality: data.airQuality?.measurements.length ? {
              atmoIndex: data.airQuality.measurements[0].aqi,
              qualifier: data.airQuality.measurements[0].aqiLabel,
              color: ''
            } : undefined
          };
          console.log("communeData : ", communeData);
          this.addMarkerToMap(communeData, null);
          if (communeData.latitude && communeData.longitude) {
            this.zoomOnMap([communeData.latitude, communeData.longitude], 11);
          }
        },
        error: (error) => {
          console.error('Error fetching commune data:', error);
        }
      });
    }
  }

/**
 * Initializes the favorite communes on the map.
 * Fetches the favorite communes for the current user and adds a marker to the map for each favorite commune.
 * If the favorite communes cannot be fetched, logs an error message to the console.
 */
  initFavorites() {
    const currentUser: AuthUser | null = this.authService.getCurrentUser();

    if (currentUser) {
      this.favoriteService.getUserFavorites(currentUser.id).subscribe({
        next: (data) => {
          data.forEach(favorite => this.geographicService.getCommuneDatas(favorite.communeInseeCode).subscribe({
            next: (data) => {
              this.addMarkerToMap(data, 'favorite');
            },
            error: (error) => {
              console.error('Error fetching favorite commune data:', error);
            }
          }))
        },
        error: (error) => {
          console.error('Error fetching favorite communes:', error);
        }
      });
    }

  }

  /**
   * Adds a marker to the map for the given commune.
   * The marker is displayed at the commune's latitude and longitude,
   * and its icon is determined by the commune's air quality index.
   * When the marker is clicked, emits the onMarkerClick event with the commune,
   * and opens a popup displaying the commune's data.
   * @param commune The commune data to add to the map.
   */
  addMarkerToMap(commune: Commune | CommuneWithAirQuality | any, prefix: string | null) {
    if (!commune.latitude || !commune.longitude) {
      return;
    }

    const atmoIndex = 'currentAirQuality' in commune
      ? commune.currentAirQuality?.atmoIndex || 0
      : commune.atmoIndex || 0;

    let icon: any = L.icon({
      iconUrl: `assets/images/${prefix ? prefix + '-' : ''}marker-${commune.atmoIndex}.png`,
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
  zoomOnMap(latLng: any, zoomLevel: number) {
    this.map.setView(latLng, zoomLevel);
  }

}
