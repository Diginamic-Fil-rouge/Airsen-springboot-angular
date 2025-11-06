import {
  Component,
  AfterViewInit,
  OnDestroy,
  Input,
  Output,
  EventEmitter,
  inject
} from '@angular/core';
import * as L from 'leaflet';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AqiStation, getAqiColor, getAqiLabel } from '../../models/aqi-station.model';
import { MapStyle } from '../../models/map-filter.model';

/**
 * Enhanced Map View Component with zoom-based clustering
 * Shows big communes at low zoom, more as you zoom in
 */
@Component({
  standalone: false,
  selector: 'app-map-view',
  templateUrl: './map-view.component.html',
  styleUrls: ['./map-view.component.scss']
})
export class MapViewComponent implements AfterViewInit, OnDestroy {
  @Input() stations: AqiStation[] = [];
  @Input() selectedStation: AqiStation | null = null;
  @Input() showHeatmap: boolean = false;
  @Input() mapStyle: MapStyle = MapStyle.STREETS;

  @Output() stationSelected = new EventEmitter<AqiStation>();

  private map: L.Map | null = null;
  private markerLayer: L.LayerGroup | null = null;
  private heatmapLayer: L.LayerGroup | null = null;
  private tileLayers: Map<MapStyle, L.TileLayer> = new Map();
  private markers: Map<string, L.Marker> = new Map();
  private destroy$ = new Subject<void>();
  private currentZoom: number = 6;

  // Population thresholds based on zoom level
  private readonly ZOOM_POPULATION_THRESHOLDS = {
    3: 1000000,  // Zoom 3-5: Very large cities (1M+)
    6: 200000,   // Zoom 6-7: Large cities (200K+)
    8: 100000,   // Zoom 8-9: Medium cities (100K+)
    10: 50000,   // Zoom 10-11: Small cities (50K+)
    12: 20000,   // Zoom 12-13: Towns (20K+)
    14: 5000,    // Zoom 14+: All communes (5K+)
  };

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initMap();
      this.setupTileLayers();
      this.addTileLayer(this.mapStyle);
      this.createMarkerLayer();
      this.updateMarkers();
    }, 0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  ngOnChanges(): void {
    if (this.map) {
      this.updateMarkers();
      this.updateMapStyle();
      this.updateHeatmap();
      this.highlightSelectedStation();
    }
  }

  /**
   * Initialize the Leaflet map
   */
  private initMap(): void {
    const mapElement = document.getElementById('map-container');
    if (!mapElement) {
      console.error('Map element not found');
      return;
    }

    this.map = L.map('map-container', {
      center: [46.603354, 1.888334], // Center of France
      zoom: 6,
      zoomControl: false, // Custom controls
      attributionControl: true
    });

    // Listen to zoom changes for dynamic clustering
    this.map.on('zoomend', () => {
      this.onZoomChanged();
    });

    // Force map to recalculate size
    setTimeout(() => {
      if (this.map) {
        this.map.invalidateSize();
      }
    }, 100);
  }

  /**
   * Handle zoom level changes - update markers based on population
   */
  private onZoomChanged(): void {
    if (!this.map) return;

    const newZoom = this.map.getZoom();

    // Only update if zoom level changed significantly
    if (Math.abs(newZoom - this.currentZoom) >= 1) {
      this.currentZoom = newZoom;
      this.updateMarkers();
    }
  }

  /**
   * Get minimum population threshold for current zoom level
   */
  private getPopulationThreshold(): number {
    const zoom = Math.floor(this.currentZoom);

    if (zoom <= 5) return this.ZOOM_POPULATION_THRESHOLDS[3];
    if (zoom <= 7) return this.ZOOM_POPULATION_THRESHOLDS[6];
    if (zoom <= 9) return this.ZOOM_POPULATION_THRESHOLDS[8];
    if (zoom <= 11) return this.ZOOM_POPULATION_THRESHOLDS[10];
    if (zoom <= 13) return this.ZOOM_POPULATION_THRESHOLDS[12];
    return this.ZOOM_POPULATION_THRESHOLDS[14];
  }

  /**
   * Setup different tile layers for map styles
   */
  private setupTileLayers(): void {
    // Streets (OpenStreetMap)
    this.tileLayers.set(
      MapStyle.STREETS,
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        minZoom: 3,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
      })
    );

    // Satellite (ESRI World Imagery)
    this.tileLayers.set(
      MapStyle.SATELLITE,
      L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
        maxZoom: 18,
        minZoom: 3,
        attribution: '&copy; <a href="https://www.esri.com/">Esri</a>'
      })
    );

    // Terrain (OpenTopoMap)
    this.tileLayers.set(
      MapStyle.TERRAIN,
      L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', {
        maxZoom: 17,
        minZoom: 3,
        attribution: '&copy; <a href="https://opentopomap.org">OpenTopoMap</a>'
      })
    );

    // Dark mode (CartoDB Dark Matter)
    this.tileLayers.set(
      MapStyle.DARK,
      L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        maxZoom: 19,
        minZoom: 3,
        attribution: '&copy; <a href="https://carto.com/">CARTO</a>'
      })
    );
  }

  /**
   * Add tile layer to map
   */
  private addTileLayer(style: MapStyle): void {
    if (!this.map) return;

    // Remove all existing layers
    this.tileLayers.forEach(layer => {
      if (this.map?.hasLayer(layer)) {
        this.map.removeLayer(layer);
      }
    });

    // Add selected layer
    const layer = this.tileLayers.get(style);
    if (layer) {
      layer.addTo(this.map);
    }
  }

  /**
   * Create marker layer
   */
  private createMarkerLayer(): void {
    if (!this.map) return;
    this.markerLayer = L.layerGroup().addTo(this.map);
  }

  /**
   * Update markers - filtered by population based on zoom level
   */
  private updateMarkers(): void {
    if (!this.map || !this.markerLayer) return;

    // Clear existing markers
    this.markerLayer.clearLayers();
    this.markers.clear();

    // Get population threshold for current zoom
    const populationThreshold = this.getPopulationThreshold();

    // Filter stations by population
    const visibleStations = this.stations.filter(station =>
      (station.population || 0) >= populationThreshold
    );

    console.log(`Zoom ${this.currentZoom}: Showing ${visibleStations.length}/${this.stations.length} stations (pop >= ${populationThreshold})`);

    // Add markers for visible stations
    visibleStations.forEach(station => {
      if (!station.latitude || !station.longitude) return;

      const marker = this.createMarker(station);
      marker.addTo(this.markerLayer!);
      this.markers.set(station.inseeCode, marker);
    });

    // Always show selected station even if below threshold
    if (this.selectedStation &&
        !this.markers.has(this.selectedStation.inseeCode) &&
        this.selectedStation.latitude &&
        this.selectedStation.longitude) {
      const marker = this.createMarker(this.selectedStation);
      marker.addTo(this.markerLayer);
      this.markers.set(this.selectedStation.inseeCode, marker);
    }
  }

  /**
   * Create enhanced marker with AQI styling
   */
  private createMarker(station: AqiStation): L.Marker {
    const color = getAqiColor(station.aqiLevel);
    const label = getAqiLabel(station.aqiLevel);

    // Create custom icon with AQI color
    const icon = L.divIcon({
      className: 'aqi-custom-marker',
      html: `
        <div class="aqi-marker" style="background-color: ${color}; border: 3px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.3);">
          <span style="color: ${station.aqiLevel === 'MODERATE' ? '#000' : '#fff'}; font-weight: bold; font-size: 12px;">
            ${station.currentAqi || '?'}
          </span>
        </div>
      `,
      iconSize: [36, 36],
      iconAnchor: [18, 18]
    });

    const marker = L.marker([station.latitude, station.longitude], { icon });

    // Add popup
    const popupContent = `
      <div style="min-width: 200px;">
        <h3 style="margin: 0 0 8px 0; color: #212121;">${station.name}</h3>
        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
          <div style="width: 12px; height: 12px; border-radius: 50%; background-color: ${color};"></div>
          <strong style="font-size: 18px; color: ${color};">${station.currentAqi}</strong>
          <span style="color: #616161;">${label}</span>
        </div>
        ${station.population ? `<p style="margin: 0 0 4px 0; font-size: 12px; color: #757575;">Population: ${station.population.toLocaleString('fr-FR')}</p>` : ''}
        <p style="margin: 0; font-size: 12px; color: #757575;">
          Dernière mise à jour: ${new Date(station.lastUpdated).toLocaleString('fr-FR')}
        </p>
      </div>
    `;

    marker.bindPopup(popupContent);

    // Click handler
    marker.on('click', () => {
      this.stationSelected.emit(station);
    });

    // Hover effects
    marker.on('mouseover', (e) => {
      const markerElement = e.target.getElement();
      if (markerElement) {
        markerElement.style.transform = 'scale(1.15)';
        markerElement.style.zIndex = '1000';
      }
    });

    marker.on('mouseout', (e) => {
      const markerElement = e.target.getElement();
      if (markerElement && station.inseeCode !== this.selectedStation?.inseeCode) {
        markerElement.style.transform = 'scale(1)';
        markerElement.style.zIndex = '400';
      }
    });

    return marker;
  }

  /**
   * Update map style
   */
  private updateMapStyle(): void {
    this.addTileLayer(this.mapStyle);
  }

  /**
   * Update heatmap overlay
   */
  private updateHeatmap(): void {
    if (!this.map) return;

    // Remove existing heatmap
    if (this.heatmapLayer) {
      this.map.removeLayer(this.heatmapLayer);
      this.heatmapLayer = null;
    }

    // Add heatmap if enabled
    if (this.showHeatmap) {
      // TODO: Implement heatmap using leaflet.heat plugin
      console.log('Heatmap feature - to be implemented with leaflet.heat');
    }
  }

  /**
   * Highlight selected station
   */
  private highlightSelectedStation(): void {
    if (!this.selectedStation) return;

    const marker = this.markers.get(this.selectedStation.inseeCode);
    if (marker && this.map) {
      // Zoom to marker
      this.map.setView(
        [this.selectedStation.latitude, this.selectedStation.longitude],
        11,
        { animate: true }
      );

      // Open popup
      marker.openPopup();

      // Highlight marker
      const markerElement = marker.getElement();
      if (markerElement) {
        markerElement.style.transform = 'scale(1.2)';
        markerElement.style.zIndex = '1001';
      }
    }
  }

  /**
   * Public method to zoom to location
   */
  public zoomToLocation(lat: number, lng: number, zoom: number = 11): void {
    if (this.map) {
      this.map.setView([lat, lng], zoom, { animate: true });
    }
  }

  /**
   * Public method to get user's current location
   */
  public goToUserLocation(): void {
    if (!this.map) return;

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;
          this.map!.setView([lat, lng], 11, { animate: true });

          // Add temporary marker for user location
          const userIcon = L.divIcon({
            className: 'user-location-marker',
            html: '<div style="width: 16px; height: 16px; background-color: #2196F3; border: 3px solid white; border-radius: 50%; box-shadow: 0 2px 8px rgba(0,0,0,0.3);"></div>',
            iconSize: [16, 16],
            iconAnchor: [8, 8]
          });

          L.marker([lat, lng], { icon: userIcon })
            .addTo(this.map!)
            .bindPopup('Votre position')
            .openPopup();
        },
        (error) => {
          console.error('Error getting user location:', error);
          alert('Impossible d\'obtenir votre position');
        }
      );
    } else {
      alert('La géolocalisation n\'est pas supportée par votre navigateur');
    }
  }

  /**
   * Public method to zoom in
   */
  public zoomIn(): void {
    if (this.map) {
      this.map.zoomIn();
    }
  }

  /**
   * Public method to zoom out
   */
  public zoomOut(): void {
    if (this.map) {
      this.map.zoomOut();
    }
  }
}
