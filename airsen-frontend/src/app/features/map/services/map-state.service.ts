import { Injectable } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { distinctUntilChanged, map, shareReplay } from 'rxjs/operators';
import { CommuneWithAirQuality, CommuneDatas } from '@/shared/models/commune.model';

export type MapViewMode = 'aqi' | 'weather';

export interface MapBounds {
  north: number;
  south: number;
  east: number;
  west: number;
}

export interface MapFilter {
  departments: string[];
  regions: string[];
  minPopulation?: number;
  aqiThreshold?: number;
}

export interface MapState {
  selectedCommune: CommuneWithAirQuality | null;
  viewMode: MapViewMode;
  mapCenter: [number, number];
  zoomLevel: number;
  bounds: MapBounds | null;
  filter: MapFilter;
  isLoading: boolean;
}

/**
 * MapStateService - Centralized RxJS-based state management for the map feature
 *
 * This service acts as a single source of truth for all map-related state,
 * coordinating communication between search, map, and sidebar components.
 *
 * Key Features:
 * - Reactive state updates using BehaviorSubjects
 * - URL synchronization for bookmarkable map positions
 * - Deduplication with distinctUntilChanged operators
 * - Performance optimization with shareReplay
 */
@Injectable({
  providedIn: 'root'
})
export class MapStateService {
  // Private BehaviorSubjects (internal state)

  private selectedCommuneSubject = new BehaviorSubject<CommuneWithAirQuality | null>(null);
  private viewModeSubject = new BehaviorSubject<MapViewMode>('aqi');
  private mapCenterSubject = new BehaviorSubject<[number, number]>([46.603354, 1.888334]); // France center
  private zoomLevelSubject = new BehaviorSubject<number>(6);
  private boundsSubject = new BehaviorSubject<MapBounds | null>(null);
  private filterSubject = new BehaviorSubject<MapFilter>({
    departments: [],
    regions: [],
    minPopulation: undefined,
    aqiThreshold: undefined
  });
  private isLoadingSubject = new BehaviorSubject<boolean>(false);
  // Public Observables (components subscribe)

  /**
   * Observable stream of selected commune
   * Deduplicates by inseeCode to prevent redundant updates
   */
  public selectedCommune$ = this.selectedCommuneSubject.asObservable().pipe(
    distinctUntilChanged((prev, curr) => prev?.inseeCode === curr?.inseeCode)
  );

  /**
   * Observable stream of view mode (AQI or Weather)
   */
  public viewMode$ = this.viewModeSubject.asObservable().pipe(
    distinctUntilChanged()
  );

  /**
   * Observable stream of map center coordinates
   */
  public mapCenter$ = this.mapCenterSubject.asObservable().pipe(
    distinctUntilChanged((prev, curr) => prev[0] === curr[0] && prev[1] === curr[1])
  );

  /**
   * Observable stream of zoom level
   */
  public zoomLevel$ = this.zoomLevelSubject.asObservable().pipe(
    distinctUntilChanged()
  );

  /**
   * Observable stream of map bounds
   */
  public bounds$ = this.boundsSubject.asObservable();

  /**
   * Observable stream of filter criteria
   */
  public filter$ = this.filterSubject.asObservable().pipe(
    distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
  );

  /**
   * Observable stream of loading state
   */
  public isLoading$ = this.isLoadingSubject.asObservable();

  /**
   * Combined observable of all state properties
   * Useful for components that need to react to multiple state changes
   */
  public mapState$: Observable<MapState> = combineLatest([
    this.selectedCommune$,
    this.viewMode$,
    this.mapCenter$,
    this.zoomLevel$,
    this.bounds$,
    this.filter$,
    this.isLoading$
  ]).pipe(
    map(([selectedCommune, viewMode, mapCenter, zoomLevel, bounds, filter, isLoading]) => ({
      selectedCommune,
      viewMode,
      mapCenter,
      zoomLevel,
      bounds,
      filter,
      isLoading
    })),
    shareReplay(1) // Cache last emitted value for late subscribers
  );

  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.initializeFromUrl();
  }

  // State Setters (Components call these)

  /**
   * Select a commune (from search, marker click, or URL)
   * Triggers updates to sidebar, map centering, and URL
   *
   * @param commune The commune to select, or null to clear selection
   */
  selectCommune(commune: CommuneWithAirQuality | null): void {
    this.selectedCommuneSubject.next(commune);

    if (commune) {
      // Update URL with selected commune
      this.updateUrl({ commune: commune.inseeCode });

      // Auto-center map on commune (components can override)
      if (commune.latitude && commune.longitude) {
        this.setMapCenter([commune.latitude, commune.longitude]);
        this.setZoomLevel(12); // Zoom in on selected commune
      }
    } else {
      // Clear selection
      this.updateUrl({ commune: null });
    }
  }

  /**
   * Toggle between AQI and Weather view modes
   *
   * @param mode The view mode to set
   */
  setViewMode(mode: MapViewMode): void {
    this.viewModeSubject.next(mode);
    this.updateUrl({ mode });
  }

  /**
   * Update map center (from map drag/pan)
   *
   * @param center Latitude and longitude coordinates
   */
  setMapCenter(center: [number, number]): void {
    this.mapCenterSubject.next(center);
  }

  /**
   * Update zoom level (from zoom controls or wheel)
   *
   * @param zoom Zoom level (typically 1-18)
   */
  setZoomLevel(zoom: number): void {
    this.zoomLevelSubject.next(zoom);
    this.updateUrl({ zoom });
  }

  /**
   * Update map bounds (for filtering visible communes)
   *
   * @param bounds Geographic bounds of visible map area
   */
  setMapBounds(bounds: MapBounds): void {
    this.boundsSubject.next(bounds);
  }

  /**
   * Update filter criteria (departments, regions, population, AQI threshold)
   * Merges with existing filter to allow partial updates
   *
   * @param filter Partial filter criteria to update
   */
  setFilter(filter: Partial<MapFilter>): void {
    const currentFilter = this.filterSubject.value;
    this.filterSubject.next({ ...currentFilter, ...filter });
  }

  /**
   * Reset filter to defaults
   */
  resetFilter(): void {
    this.filterSubject.next({
      departments: [],
      regions: [],
      minPopulation: undefined,
      aqiThreshold: undefined
    });
  }

  /**
   * Set loading state (for API calls)
   *
   * @param loading Whether data is currently being loaded
   */
  setLoading(loading: boolean): void {
    this.isLoadingSubject.next(loading);
  }

  // State Getters (Synchronous access)

  /**
   * Get currently selected commune (synchronous)
   *
   * @returns The currently selected commune or null
   */
  getCurrentCommune(): CommuneWithAirQuality | null {
    return this.selectedCommuneSubject.value;
  }

  /**
   * Get current view mode (synchronous)
   *
   * @returns Current view mode (aqi or weather)
   */
  getCurrentViewMode(): MapViewMode {
    return this.viewModeSubject.value;
  }

  /**
   * Get current zoom level (synchronous)
   *
   * @returns Current zoom level
   */
  getCurrentZoom(): number {
    return this.zoomLevelSubject.value;
  }

  /**
   * Get current filter (synchronous)
   *
   * @returns Current filter criteria
   */
  getCurrentFilter(): MapFilter {
    return this.filterSubject.value;
  }

  // URL Synchronization (Bookmarking)

  /**
   * Initialize state from URL query parameters
   * Called on service construction to restore state from bookmarked URLs
   *
   * @private
   */
  private initializeFromUrl(): void {
    // Parse query params on load
    this.route.queryParams.subscribe(params => {
      // Note: Commune loading is handled by parent component
      // This service only manages state, not API calls

      if (params['mode']) {
        const mode = params['mode'] as MapViewMode;
        if (mode === 'aqi' || mode === 'weather') {
          this.setViewMode(mode);
        }
      }

      if (params['zoom']) {
        const zoom = parseInt(params['zoom'], 10);
        if (!isNaN(zoom) && zoom >= 1 && zoom <= 18) {
          this.setZoomLevel(zoom);
        }
      }

      if (params['lat'] && params['lng']) {
        const lat = parseFloat(params['lat']);
        const lng = parseFloat(params['lng']);
        if (!isNaN(lat) && !isNaN(lng)) {
          this.setMapCenter([lat, lng]);
        }
      }
    });
  }

  /**
   * Update URL query parameters based on state changes
   * Uses replaceUrl to avoid polluting browser history
   *
   * @private
   * @param params Parameters to update in the URL
   */
  private updateUrl(params: { commune?: string | null; mode?: MapViewMode; zoom?: number }): void {
    const currentParams = this.route.snapshot.queryParams;
    const updatedParams = { ...currentParams };

    if (params.commune !== undefined) {
      if (params.commune) {
        updatedParams['commune'] = params.commune;
      } else {
        delete updatedParams['commune'];
      }
    }

    if (params.mode) {
      updatedParams['mode'] = params.mode;
    }

    if (params.zoom) {
      updatedParams['zoom'] = params.zoom.toString();
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: updatedParams,
      queryParamsHandling: 'merge',
      replaceUrl: true // Don't add to browser history
    });
  }

  // Utility Methods

  /**
   * Check if a commune is currently selected
   *
   * @param inseeCode INSEE code of the commune to check
   * @returns True if the commune is selected
   */
  isSelected(inseeCode: string): boolean {
    return this.selectedCommuneSubject.value?.inseeCode === inseeCode;
  }

  /**
   * Clear all state (reset to defaults)
   * Useful for cleanup or resetting the map
   */
  reset(): void {
    this.selectedCommuneSubject.next(null);
    this.viewModeSubject.next('aqi');
    this.mapCenterSubject.next([46.603354, 1.888334]);
    this.zoomLevelSubject.next(6);
    this.boundsSubject.next(null);
    this.resetFilter();
    this.isLoadingSubject.next(false);
  }
}
