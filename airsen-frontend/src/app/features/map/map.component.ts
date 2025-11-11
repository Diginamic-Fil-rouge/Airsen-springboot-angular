import { Component, OnInit } from "@angular/core";
import { CommuneDataService } from "@/core/services/commune-data.service";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";
import { Observable } from "rxjs";

/**
 * Map Container Component
 *
 * Purpose: Main container for the interactive air quality map with progressive loading
 * Responsibilities:
 * - Fetch commune data from backend API with population filtering
 * - Implement progressive loading strategy based on zoom levels
 * - Manage loading states and error handling
 * - Pass data to LeafletMapComponent for rendering
 * - Handle user interactions (commune selection, zoom changes)
 *
 * Architecture Role:
 * Acts as a smart component (container) that:
 * - Manages application state and data flow
 * - Communicates with CommuneDataService
 * - Delegates presentation logic to LeafletMapComponent
 *
 * Progressive Loading Strategy:
 * - Zoom 6 (initial): 50K+ population → ~18 communes (~5KB)
 * - Zoom 8-9: 20K+ population → ~68 communes (~15KB)
 * - Zoom 10-11: 10K+ population → ~163 communes (~35KB)
 * - Zoom 12+: All communes → ~10,381 communes (~2MB)
 *
 * Performance Optimization:
 * This prevents loading 10K+ communes on initial page load, reducing
 * data transfer from ~2MB to ~5KB (400x improvement) and enabling
 * smooth rendering without browser freeze.
 */
@Component({
  standalone: false,
  selector: "app-map",
  templateUrl: "./map.component.html",
  styleUrls: ["./map.component.scss"],
})
export class MapComponent implements OnInit {
  communes: CommuneWithAirQuality[] = [];
  isLoading$: Observable<boolean>;

  // Progressive loading state tracking
  private currentZoom = 6;
  private loadedPopulationThreshold = 50000; // Track what we've already loaded

  constructor(private communeDataService: CommuneDataService) {
    this.isLoading$ = this.communeDataService.loading$;
  }

  ngOnInit(): void {
    // INITIAL LOAD: Only major cities (50K+ population)
    console.log("[Map] Initial load: Fetching major cities (pop >= 50000)");
    this.loadCommunes(50000);
  }

  /**
   * Loads communes from backend with optional population filter.
   * Updates component state and tracks loaded threshold for progressive loading.
   */
  private loadCommunes(minPopulation?: number): void {
    this.communeDataService.getAllCommunesWithCoordinates(minPopulation).subscribe({
      next: (communes) => {
        this.communes = communes;
        this.loadedPopulationThreshold = minPopulation || 0;

        const filterMsg = minPopulation ? `(pop >= ${minPopulation})` : "(all)";
        console.log(`[Map]  Loaded ${communes.length} communes ${filterMsg}`);
      },
      error: (error) => {
        console.error("[Map]  Failed to load communes:", error);
      },
    });
  }

  /**
   * PROGRESSIVE LOADING: Load more communes as user zooms in
   *
   * Strategy:
   * - Zoom 6 (initial): 50K+ population → ~18 communes (~5KB)
   * - Zoom 8-9: 20K+ population → ~68 communes (~15KB)
   * - Zoom 10-11: 10K+ population → ~163 communes (~35KB)
   * - Zoom 12+: All communes → ~10,381 communes (~2MB)
   *
   * Only loads when zooming IN (not out) and when we need more data.
   * Prevents redundant API calls by tracking loadedPopulationThreshold.
   */
  onMapZoomChanged(zoom: number): void {
    const previousZoom = this.currentZoom;
    this.currentZoom = zoom;

    console.log(`[Map] Zoom: ${previousZoom} → ${zoom}`);

    // Only load more data when zooming in (not out)
    if (zoom <= previousZoom) {
      console.log("[Map] Zooming out, no new data needed");
      return;
    }

    // Determine new threshold based on zoom level
    let newThreshold: number | undefined;

    if (zoom >= 12 && this.loadedPopulationThreshold > 0) {
      // Load ALL communes at zoom 12+
      newThreshold = undefined;
      console.log("[Map] Zoom >= 12: Loading ALL communes");
    } else if (zoom >= 10 && this.loadedPopulationThreshold > 10000) {
      // Load 10K+ communes at zoom 10-11
      newThreshold = 10000;
      console.log("[Map] Zoom >= 10: Loading 10K+ population communes");
    } else if (zoom >= 8 && this.loadedPopulationThreshold > 20000) {
      // Load 20K+ communes at zoom 8-9
      newThreshold = 20000;
      console.log("[Map] Zoom >= 8: Loading 20K+ population communes");
    } else {
      console.log("[Map] No new data threshold reached");
      return;
    }

    // Load more communes with new threshold
    this.loadCommunes(newThreshold);
  }

  /**
   * Handles commune marker click events.
   * Phase 2 will show sidebar with detailed commune information.
   */
  onCommuneClicked(commune: CommuneWithAirQuality): void {
    console.log("[Map] Commune clicked:", commune.name, commune.currentAirQuality);
    // Phase 2: Show sidebar with commune details, pollutants, weather, etc.
  }
}
