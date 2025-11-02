import {
  Component,
  AfterViewInit,
  OnDestroy,
  input,
  inject,
  Output,
  EventEmitter,
  ViewContainerRef,
  Injector,
  ComponentRef,
  createComponent,
  effect,
} from "@angular/core";
import * as L from "leaflet";
import { Observable } from "rxjs";
import { Commune, CommuneWithAirQuality } from "@/shared/models";
import { GeographicService } from "../services/geographic.service";
import { FavoriteService } from "../../favorites/services/favorite.service";
import { AuthService } from "../../../core/auth/services/auth.service";
import { AuthUser } from "@/app/core/auth/models/auth.model";

@Component({
  standalone: false,
  selector: "app-map-view",
  templateUrl: "./map-view.component.html",
  styleUrls: ["./map-view.component.scss"],
})
export class MapViewComponent implements AfterViewInit, OnDestroy {
  private map: any;
  private geographicService = inject(GeographicService);
  private favoriteService = inject(FavoriteService);
  private authService = inject(AuthService);
  markersInitialized = false;
  communes = input<Observable<Commune[]>>();
  favoriteCommunes: Commune[] | null = null;
  communeSearched = input<Commune | null>();
  @Output() onMarkerClick = new EventEmitter<any>();

  constructor(private viewContainerRef: ViewContainerRef, private injector: Injector) {
    // Set up effects to react to signal input changes
    effect(() => {
      const communes = this.communes();
      if (communes) {
        this.initMarkers();
      }
    });

    effect(() => {
      const searchedCommune = this.communeSearched();
      if (searchedCommune) {
        this.initMarkers();
      }
    });
  }


  ngAfterViewInit(): void {
    // Small timeout to ensure DOM is ready
    setTimeout(() => {
      this.initMap();
      this.initMarkers();
      this.initFavorites();
    }, 0);
  }

  ngOnDestroy(): void {
    // Clean up map instance
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
    this.markersInitialized = false;
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
    // Clean up existing map instance if any
    if (this.map) {
      this.map.remove();
    }

    const mapElement = document.getElementById("map");
    if (!mapElement) {
      console.error("Map element not found");
      return;
    }

    this.map = L.map("map", {
      // coordinates of France
      center: [47.0, 1.5231],
      zoom: 6,
    });

    const tiles = L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 18,
      minZoom: 3,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    });

    tiles.addTo(this.map);

    // Force map to recalculate size
    setTimeout(() => {
      if (this.map) {
        this.map.invalidateSize();
      }
    }, 100);
  }

  /**
   * Initializes the markers on the map.
   * If the markers have not been initialized yet, it fetches the communes from the input observable and adds a marker to the map for each commune.
   * If a commune has been searched, adds a marker for it and zooms to its location using its existing coordinates.
   * The markers are only initialized once, and then the function does nothing.
   */
  initMarkers() {
    // Wait for map to be initialized
    if (!this.map) {
      console.warn("Map not initialized yet, skipping marker initialization");
      return;
    }

    if (!this.markersInitialized) {
      this.communes()?.forEach((communes) => {
        console.log("commune : ", communes);
        communes.forEach((commune) => {
          this.addMarkerToMap(commune, null);
        });
      });
      this.markersInitialized = true;
    }

    if (this.communeSearched()) {
      const searchedCommune = this.communeSearched();

      if (searchedCommune && searchedCommune.latitude && searchedCommune.longitude) {
        this.geographicService.getCommuneDatas(searchedCommune.inseeCode).subscribe({
          next: (data) => {
            const communeData: CommuneWithAirQuality = {
              id: searchedCommune.id,
              inseeCode: searchedCommune.inseeCode,
              name: searchedCommune.name,
              population: searchedCommune.population || 0,
              latitude: searchedCommune.latitude,
              longitude: searchedCommune.longitude,
              department: searchedCommune.department || {
                id: 0,
                code: "",
                name: "",
              },
              currentAirQuality: data.airQuality
                ? {
                    atmoIndex: data.airQuality.atmoIndex,
                    qualifier: data.airQuality.qualifier,
                    color: data.airQuality.color,
                  }
                : undefined,
            };
            console.log("communeData : ", communeData);
            this.addMarkerToMap(communeData, null);
            this.zoomOnMap([communeData.latitude, communeData.longitude], 11);
          },
          error: (error) => {
            console.error("Error fetching commune data:", error);
          },
        });
      } else {
        console.warn("Searched commune does not have coordinates:", searchedCommune);
      }
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
        next: (data: any[]) => {
          data.forEach((favorite: any) => this.geographicService.getCommuneDatas(favorite.communeInseeCode).subscribe({
            next: (communeData: any) => {
              this.addMarkerToMap(communeData, 'favorite');
            },
            error: (error: any) => {
              console.error('Error fetching favorite commune data:', error);
            }
          }))
        },
        error: (error: any) => {
          console.error("Error fetching favorite communes:", error);
        },
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
    if (!this.map) {
      console.warn("Cannot add marker: map not initialized");
      return;
    }

    if (!commune.latitude || !commune.longitude) {
      return;
    }

    // Determine atmoIndex from various possible sources
    let atmoIndex = 0;

    // Check for currentAirQuality (from CommuneWithAirQuality)
    if ("currentAirQuality" in commune && commune.currentAirQuality) {
      atmoIndex = commune.currentAirQuality.atmoIndex;
    }
    // Check for airQuality directly (from CommuneDatas/favorites)
    else if ("airQuality" in commune && commune.airQuality) {
      atmoIndex = commune.airQuality.atmoIndex;
    }
    // Fallback to commune.atmoIndex if available
    else if ("atmoIndex" in commune) {
      atmoIndex = commune.atmoIndex;
    }

    let icon: any = L.icon({
      iconUrl: `assets/images/${prefix ? prefix + "-" : ""}marker-${atmoIndex}.png`,
      iconSize: [20, 20],
      iconAnchor: [10, 10],
      popupAnchor: [-3, -76],
    });

    const marker = L.marker([commune.latitude, commune.longitude], { icon: icon }).addTo(this.map);

    marker.on("click", () => {
      this.onMarkerClick.emit(commune);
    });
  }

  /**
   * Zooms the map to the given latitude and longitude at the given zoom level.
   * @param {any} latLng - The latitude and longitude to zoom to.
   * @param {number} zoomLevel - The zoom level to use.
   */
  zoomOnMap(latLng: any, zoomLevel: number) {
    if (!this.map) {
      console.warn("Cannot zoom: map not initialized");
      return;
    }
    this.map.setView(latLng, zoomLevel);
  }
}
