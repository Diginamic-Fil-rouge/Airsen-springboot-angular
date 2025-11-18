import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { BreakpointObserver } from "@angular/cdk/layout";
import { CommuneDataService } from "@/core/services/commune-data.service";
import { Commune, CommuneWithAirQuality } from "@/shared/models/commune.model";
import { BehaviorSubject, Observable, Subject, combineLatest } from "rxjs";
import { takeUntil, filter } from "rxjs/operators";
import { MapSidebarDisplayMode } from "./components/map-sidebar/map-sidebar.types";

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
export class MapComponent implements OnInit, OnDestroy {
  communes: CommuneWithAirQuality[] = [];
  isLoading$: Observable<boolean>;
  error$: Observable<string | null>;
  selectedCommune: CommuneWithAirQuality | null = null;
  isSidebarOpen = true;
  sidebarDisplayMode: MapSidebarDisplayMode = "desktop";

  // Progressive loading state tracking
  private currentZoom = 6;
  private loadedPopulationThreshold = 50000; // Track what we've already loaded
  private destroy$ = new Subject<void>();
  private isProgrammaticZoom = false; // Flag to skip progressive loading during search zoom
  private communesLoaded$ = new BehaviorSubject<boolean>(false); // Track when communes are loaded

  private readonly breakpointQueries = {
    desktop: "(min-width: 1024px)",
    tablet: "(min-width: 640px) and (max-width: 1023px)",
    mobile: "(max-width: 639px)",
  };

  constructor(
    private communeDataService: CommuneDataService,
    private breakpointObserver: BreakpointObserver,
    private route: ActivatedRoute
  ) {
    this.isLoading$ = this.communeDataService.loading$;
    this.error$ = this.communeDataService.error$;
  }

  ngOnInit(): void {
    this.setupSidebarModeListener();

    // INITIAL LOAD: Only major cities (50K+ population)
    console.log("[Map] Initial load: Fetching major cities (pop >= 50000)");
    this.loadCommunes(50000);

    // Handle query params from favorites navigation
    // CRITICAL: Use combineLatest to wait for BOTH communes to load AND query params to be available
    combineLatest([
      this.route.queryParams,
      this.communesLoaded$.pipe(filter(loaded => loaded)) // Only proceed when communes are loaded
    ])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([params, _]) => {
        const communeId = params["commune"];
        const shouldOpenSidebar = params["openSidebar"] === "true";

        if (communeId) {
          console.log(`[Map] Query param detected: commune=${communeId}, openSidebar=${shouldOpenSidebar}`);

          // Find commune in loaded list and select it
          const commune = this.communes.find((c) => c.inseeCode === communeId);
          if (commune) {
            console.log(`[Map] Commune found in list: ${commune.name}, selecting...`);
            this.onCommuneClicked(commune);
            if (shouldOpenSidebar) {
              this.openSidebar();
            }
          } else {
            console.warn(`[Map] Commune with inseeCode ${communeId} not found in loaded communes list`);
          }
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

        // Notify that communes are loaded (enables query param processing)
        this.communesLoaded$.next(true);
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

    // Skip progressive loading if zoom is from search/commune selection (programmatic)
    if (this.isProgrammaticZoom) {
      console.log("[Map] Programmatic zoom from search, skipping progressive loading");
      this.isProgrammaticZoom = false; // Reset flag
      return;
    }

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
   * Fetches detailed commune data including pollutant breakdown
   * and displays it in the sidebar.
   */
  onCommuneClicked(commune: CommuneWithAirQuality): void {
    console.log("[Map] Commune clicked:", commune.name, "Fetching detailed data...");

    // Set flag to prevent progressive loading when map zooms to clicked commune
    this.isProgrammaticZoom = true;

    // Fetch detailed commune data including pollutants
    this.communeDataService.getCommuneDetail(commune.inseeCode).subscribe({
      next: (detailedCommune) => {
        console.log("[Map] Detailed data loaded:", detailedCommune.pollutants);
        this.selectedCommune = detailedCommune;

        if (this.sidebarDisplayMode !== "desktop") {
          this.isSidebarOpen = true;
        }
      },
      error: (error) => {
        console.error("[Map] Failed to load commune detail:", error);
        // Fallback: show basic commune data without pollutants
        this.selectedCommune = commune;

        if (this.sidebarDisplayMode !== "desktop") {
          this.isSidebarOpen = true;
        }
      },
    });
  }

  onSidebarOpenChange(open: boolean): void {
    if (this.sidebarDisplayMode === "desktop") {
      this.isSidebarOpen = true;
      return;
    }

    this.isSidebarOpen = open;
  }

  onSidebarClearSelection(): void {
    this.selectedCommune = null;
  }

  onSearchCommuneSelected(commune: CommuneWithAirQuality): void {
    console.log("[Map] Search commune selected:", commune.name, "Fetching detailed data...");

    // Set flag to prevent progressive loading when map zooms to selected commune
    this.isProgrammaticZoom = true;

    this.communeDataService
      .getCommuneDetail(commune.inseeCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (detail) => {
          console.log("[Map] Detailed data from search loaded:", detail.pollutants);
          this.selectedCommune = detail;
          if (this.sidebarDisplayMode !== "desktop") {
            this.isSidebarOpen = true;
          }
        },
        error: (error) => {
          console.error("[Map] Failed to load commune from search:", error);
          // Fallback: show basic commune data
          this.selectedCommune = commune;
          if (this.sidebarDisplayMode !== "desktop") {
            this.isSidebarOpen = true;
          }
        },
      });
  }

  openSidebar(): void {
    this.isSidebarOpen = true;
  }

  /**
   * Retries loading communes after an error.
   * Clears error state and reloads initial communes.
   */
  retryLoadCommunes(): void {
    console.log("[Map] Retrying commune load after error");
    this.communeDataService.clearError();
    this.loadCommunes(50000);
  }

  get isMobileView(): boolean {
    return this.sidebarDisplayMode === "mobile";
  }

  get isTabletView(): boolean {
    return this.sidebarDisplayMode === "tablet";
  }

  get isOverlayMode(): boolean {
    return this.sidebarDisplayMode !== "desktop";
  }

  private setupSidebarModeListener(): void {
    this.breakpointObserver
      .observe(Object.values(this.breakpointQueries))
      .pipe(takeUntil(this.destroy$))
      .subscribe((state) => {
        const previousMode = this.sidebarDisplayMode;

        let nextMode: MapSidebarDisplayMode = "desktop";
        if (state.breakpoints[this.breakpointQueries.desktop]) {
          nextMode = "desktop";
        } else if (state.breakpoints[this.breakpointQueries.tablet]) {
          nextMode = "tablet";
        } else {
          nextMode = "mobile";
        }

        this.sidebarDisplayMode = nextMode;

        if (nextMode === "desktop") {
          this.isSidebarOpen = true;
          return;
        }

        if (previousMode !== nextMode) {
          this.isSidebarOpen = false;
        }
      });
  }
}
