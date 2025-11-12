import {
  Component,
  OnInit,
  AfterViewInit,
  OnDestroy,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
} from "@angular/core";
import * as L from "leaflet";
import "leaflet.markercluster";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";

/**
 * Leaflet Map Component
 *
 * Purpose: Core map rendering and interaction logic using Leaflet
 * Responsibilities:
 * - Initialize Leaflet map with OpenStreetMap tiles
 * - Render color-coded AQI markers for communes
 * - Handle marker clustering for performance
 * - Manage map interactions (zoom, pan, marker clicks)
 * - Create popups with commune information
 *
 * Architecture Role:
 * Acts as a presentation component (dumb component) that:
 * - Receives data via @Input properties
 * - Emits events via @Output for parent communication
 * - Focuses solely on map rendering and user interaction
 * - No direct service communication
 *
 * Third-party Integration:
 * - Leaflet: Core mapping functionality
 * - leaflet.markercluster: Performance optimization for large datasets
 * - Custom marker styling aligned with AIRSEN design system
 *
 * Performance Considerations:
 * - Marker clustering prevents DOM overload
 * - Efficient marker creation and updates
 * - Memory management (cleanup on destroy)
 * - Optimized for 10K+ commune dataset
 */
@Component({
  standalone: false,
  selector: "app-leaflet-map",
  templateUrl: "./leaflet-map.component.html",
  styleUrls: ["./leaflet-map.component.scss"],
})
export class LeafletMapComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {
  @Input() communes: CommuneWithAirQuality[] = [];
  @Input() selectedCommune: CommuneWithAirQuality | null = null;
  @Output() communeClicked = new EventEmitter<CommuneWithAirQuality>();
  @Output() zoomChanged = new EventEmitter<number>();

  private map!: L.Map;
  private markerClusterGroup!: L.MarkerClusterGroup;

  constructor() {}

  ngOnInit(): void {
    // Component initialization - lifecycle hook for setup
  }

  ngAfterViewInit(): void {
    // Initialize map after view is ready
    this.initializeMap();
    this.setupMarkerClustering();

    // Small delay to ensure DOM is ready
    setTimeout(() => {
      if (this.communes.length > 0) {
        this.renderMarkers();
      }
    }, 100);
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Re-render markers when communes input changes (progressive loading)
    if (changes["communes"] && !changes["communes"].firstChange && this.map) {
      console.log(`[LeafletMap] Updating markers (${this.communes.length} communes)`);
      this.renderMarkers();
    }

    // Zoom to selected commune when it changes
    if (changes["selectedCommune"]) {
      const commune = changes["selectedCommune"].currentValue;
      console.log('[LeafletMap] selectedCommune changed:', commune?.name, 'Map exists:', !!this.map);
      console.log('[LeafletMap] Commune coordinates:', commune?.latitude, commune?.longitude);

      if (commune && commune.latitude && commune.longitude) {
        if (this.map) {
          console.log(`[LeafletMap] Zooming to selected commune: ${commune.name}`);
          this.zoomToLocation(commune.latitude, commune.longitude);
        } else {
          console.warn('[LeafletMap] Cannot zoom - map not initialized yet');
        }
      }
    }
  }

  /**
   * Initializes Leaflet map with OpenStreetMap tiles and France-centered view
   */
  private initializeMap(): void {
    // Initialize Leaflet map centered on France
    this.map = L.map("map-container", {
      center: [46.603354, 1.888334], // France geographic center
      zoom: 6,
      zoomControl: false,
    });

    // Add OpenStreetMap tile layer
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
    }).addTo(this.map);

    // Add zoom control to bottom right
    L.control.zoom({ position: "bottomright" }).addTo(this.map);

    // Emit zoom changes for progressive loading
    this.map.on("zoomend", () => {
      const zoom = this.map.getZoom();
      console.log(`[LeafletMap] Zoom changed to ${zoom}`);
      this.zoomChanged.emit(zoom);
    });

    console.log("[LeafletMap] Map initialized");
  }

  /**
   * Sets up marker clustering for performance with large datasets
   */
  private setupMarkerClustering(): void {
    this.markerClusterGroup = L.markerClusterGroup({
      maxClusterRadius: 50,
      spiderfyOnMaxZoom: true,
      showCoverageOnHover: false,
      zoomToBoundsOnClick: true,
      iconCreateFunction: (cluster) => this.createClusterIcon(cluster),
    });

    this.map.addLayer(this.markerClusterGroup);
  }

  /**
   * Renders all commune markers on the map with air quality indicators
   */
  private renderMarkers(): void {
    // Clear existing markers
    this.markerClusterGroup.clearLayers();

    let markersAdded = 0;

    // Create markers for each commune
    this.communes.forEach((commune) => {
      if (commune.latitude && commune.longitude && commune.currentAirQuality) {
        const marker = this.createMarker(commune);
        this.markerClusterGroup.addLayer(marker);
        markersAdded++;
      }
    });

    console.log(`[LeafletMap] Rendered ${markersAdded} markers`);
  }

  /**
   * Creates individual marker with custom AQI-colored icon.
   * Uses color provided by backend to avoid duplicate logic.
   */
  private createMarker(commune: CommuneWithAirQuality): L.Marker {
    const atmoIndex = commune.currentAirQuality?.atmoIndex || 0;
    const aqiColor = commune.currentAirQuality?.color || "#CCCCCC"; // Fallback to gray if no color

    // Custom SVG circle marker
    const icon = L.divIcon({
      html: `
        <div class="aqi-marker" style="background-color: ${aqiColor}">
          <span class="aqi-value">${atmoIndex || "?"}</span>
        </div>
      `,
      className: "custom-marker",
      iconSize: [32, 32],
      iconAnchor: [16, 16],
    });

    const marker = L.marker([commune.latitude!, commune.longitude!], { icon });

    // Popup with basic info
    marker.bindPopup(this.createPopupContent(commune));

    // Click handler
    marker.on("click", () => {
      this.communeClicked.emit(commune);
    });

    return marker;
  }

  /**
   * Creates custom cluster icon showing number of grouped markers
   */
  private createClusterIcon(cluster: any): L.DivIcon {
    const childCount = cluster.getChildCount();
    return L.divIcon({
      html: `
        <div class="marker-cluster-custom">
          <span class="cluster-count">${childCount}</span>
        </div>
      `,
      className: "marker-cluster",
      iconSize: [40, 40],
    });
  }

  /**
   * Creates popup HTML content for commune information display
   */
  private createPopupContent(commune: CommuneWithAirQuality): string {
    const atmoIndex = commune.currentAirQuality?.atmoIndex || "N/A";
    const qualifier = commune.currentAirQuality?.qualifier || "Inconnu";
    const population = commune.population?.toLocaleString("fr-FR") || "N/A";

    return `
      <div class="marker-popup">
        <h3>${commune.name}</h3>
        <p><strong>Indice ATMO:</strong> ${atmoIndex} (${qualifier})</p>
        <p><strong>Population:</strong> ${population}</p>
      </div>
    `;
  }

  /**
   * Zoom and pan the map to a specific location
   * @param latitude Latitude coordinate
   * @param longitude Longitude coordinate
   * @param zoomLevel Target zoom level (default: 13)
   */
  private zoomToLocation(latitude: number, longitude: number, zoomLevel: number = 13): void {
    console.log(`[LeafletMap] zoomToLocation called - lat: ${latitude}, lng: ${longitude}, zoom: ${zoomLevel}`);
    console.log('[LeafletMap] Map exists:', !!this.map);

    if (this.map) {
      console.log('[LeafletMap] Closing any open popups...');
      this.map.closePopup();

      console.log('[LeafletMap] Calling map.flyTo for smooth zoom...');
      this.map.flyTo([latitude, longitude], zoomLevel, {
        animate: true,
        duration: 1.5,
      });
      console.log('[LeafletMap] Map flyTo initiated');
    } else {
      console.warn('[LeafletMap] Map not initialized, cannot zoom');
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }
}
