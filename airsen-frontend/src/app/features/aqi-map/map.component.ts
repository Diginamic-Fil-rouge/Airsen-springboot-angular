import { Component, OnInit, OnDestroy, inject, ViewChild } from "@angular/core";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { MatSnackBar } from "@angular/material/snack-bar";
import { MapService } from "./services/map.service";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";
import { MapFilter, PollutantType, TimeRange, MapStyle } from "./models/map-filter.model";
import { MapViewComponent } from "./components/map-view/map-view.component";
import { ExportDataService } from "@/core/services/export-data.service";
import { PdfGenerationService } from "@/core/services/pdf-generation.service";
import { AuthService } from "@/core/auth/services/auth.service";
import { AuthUser } from "@/core/auth/models/auth.model";
import { GeographicService } from "./services/geographic.service";
import { Commune } from "@/shared/models/commune.model";
import { WeatherService } from "./services/weather.service";
import { AirQualityService } from "./services/air-quality.service";
import { Weather } from "@/shared/models/weather.model";
import { AirQuality } from "@/shared/models/air-quality.model";
import { firstValueFrom } from "rxjs";

@Component({
  standalone: false,
  selector: "app-map",
  templateUrl: "./map.component.html",
  styleUrls: ["./map.component.scss"],
})
export class MapComponent implements OnInit, OnDestroy {
  private mapService = inject(MapService);
  private exportDataService = inject(ExportDataService);
  private pdfGenerationService = inject(PdfGenerationService);
  private snackBar = inject(MatSnackBar);
  private authService = inject(AuthService);
  private router = inject(Router);
  private geographicService = inject(GeographicService);
  private weatherService = inject(WeatherService);
  private airQualityService = inject(AirQualityService);
  private destroy$ = new Subject<void>();

  @ViewChild("mapView") mapView!: MapViewComponent;

  // Component State
  isLoading = true;
  isSidePanelOpen = true;
  isMobileView = false;
  selectedCommune: CommuneWithAirQuality | null = null;
  communes: CommuneWithAirQuality[] = [];
  currentFilter: MapFilter | null = null;
  isExportingPDF = false;
  mapStyleDefault = MapStyle.STREETS;

  // Authentication State
  currentUser: AuthUser | null = null;

  // Search State
  searchQuery: string = "";
  searchResults: Commune[] | null = null;

  // Detailed Data State
  weatherDetails: Weather | null = null;
  airQualityDetails: AirQuality | null = null;
  isLoadingDetails = false;

  ngOnInit(): void {
    this.checkMobileView();
    this.loadUserData();
    this.setupSubscriptions();
    this.loadCommunes();

    // Listen for window resize
    window.addEventListener("resize", () => this.checkMobileView());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    window.removeEventListener("resize", () => this.checkMobileView());
  }

  /**
   * Load and verify user authentication
   * Redirects to login if user is not authenticated
   */
  private loadUserData(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.currentUser = user;

      // Authentication guard - redirect if not authenticated
      if (!this.currentUser) {
        console.warn("User not authenticated, redirecting to login");
        this.router.navigate(["/auth/login"]);
      }
    });
  }

  /**
   * Setup subscriptions to service observables
   */
  private setupSubscriptions(): void {
    // Subscribe to selected commune and fetch detailed data when commune is selected
    this.mapService.selectedCommune$.pipe(takeUntil(this.destroy$)).subscribe((commune) => {
      this.selectedCommune = commune;

      // Fetch detailed data when a commune is selected
      if (commune) {
        this.fetchDetailedData(commune.inseeCode);
      } else {
        // Clear detailed data when no commune is selected
        this.weatherDetails = null;
        this.airQualityDetails = null;
        this.isLoadingDetails = false;
      }
    });

    // Subscribe to communes list
    this.mapService.communes$.pipe(takeUntil(this.destroy$)).subscribe((communes) => {
      this.communes = communes;
    });

    // Subscribe to filter changes
    this.mapService.filter$.pipe(takeUntil(this.destroy$)).subscribe((filter) => {
      this.currentFilter = filter;
    });
  }

  /**
   * Load all communes with air quality data
   */
  private loadCommunes(): void {
    this.isLoading = true;
    this.mapService
      .loadCommunes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isLoading = false;
        },
        error: (error) => {
          console.error("Error loading communes:", error);
          this.isLoading = false;
        },
      });
  }

  /**
   * Check if viewport is mobile size
   */
  private checkMobileView(): void {
    this.isMobileView = window.innerWidth < 768;
    // Auto-close side panel on mobile
    if (this.isMobileView) {
      this.isSidePanelOpen = false;
    }
  }

  /**
   * Toggle side panel visibility
   */
  toggleSidePanel(): void {
    this.isSidePanelOpen = !this.isSidePanelOpen;
  }

  /**
   * Handle commune selection from map
   */
  onCommuneSelected(commune: CommuneWithAirQuality): void {
    this.mapService.selectCommune(commune);

    // On mobile, open bottom sheet with details
    if (this.isMobileView) {
      this.isSidePanelOpen = true;
    }
  }

  /**
   * Handle filter changes from side panel
   */
  onFilterChanged(filter: Partial<MapFilter>): void {
    this.mapService.updateFilter(filter);
  }

  /**
   * Close commune details
   */
  closeCommuneDetails(): void {
    this.mapService.selectCommune(null);
    // Clear detailed data when closing panel
    this.weatherDetails = null;
    this.airQualityDetails = null;
    this.isLoadingDetails = false;
    // Also clear search results when closing details
    this.closeSearchResults();
  }

  /**
   * Map control methods
   */
  goToUserLocation(): void {
    if (this.mapView) {
      this.mapView.goToUserLocation();
    }
  }

  zoomIn(): void {
    if (this.mapView) {
      this.mapView.zoomIn();
    }
  }

  zoomOut(): void {
    if (this.mapView) {
      this.mapView.zoomOut();
    }
  }

  /**
   * Filter control handlers
   */
  onPollutantChanged(pollutant: PollutantType): void {
    this.mapService.updateFilter({ pollutantType: pollutant });
  }

  onTimeRangeChanged(timeRange: TimeRange): void {
    this.mapService.updateFilter({ timeRange });
  }

  onHeatmapToggled(showHeatmap: boolean): void {
    this.mapService.updateFilter({ showHeatmap });
  }

  onLayerChanged(mapStyle: MapStyle): void {
    this.mapService.updateFilter({ mapStyle });
  }

  /**
   * Add selected commune to favorites
   */
  addToFavorites(): void {
    if (!this.selectedCommune) {
      this.snackBar.open("Veuillez sélectionner une commune", "Fermer", { duration: 3000 });
      return;
    }

    // TODO: Implement favorites service
    this.snackBar.open(`${this.selectedCommune.name} ajoutée aux favoris`, "Fermer", {
      duration: 3000,
    });
  }

  /**
   * Export selected commune data to PDF
   */
  async exportToPDF(): Promise<void> {
    if (!this.selectedCommune) {
      this.snackBar.open("Veuillez sélectionner une commune pour exporter", "Fermer", {
        duration: 3000,
      });
      return;
    }

    this.isExportingPDF = true;

    try {
      await firstValueFrom(
        this.exportDataService.exportAsPDF(this.selectedCommune.inseeCode, this.selectedCommune.name)
      );
      this.snackBar.open("PDF généré avec succès", "Fermer", { duration: 3000 });
    } catch (error) {
      console.error("Error generating PDF:", error);
      this.snackBar.open("Erreur lors de la génération du PDF", "Fermer", { duration: 3000 });
    } finally {
      this.isExportingPDF = false;
    }
  }

  /**
   * Logout user and redirect to login page
   */
  logout(): void {
    this.authService
      .logout()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.snackBar.open("Déconnexion réussie", "Fermer", { duration: 2000 });
          this.router.navigate(["/auth/login"]);
        },
        error: (error) => {
          console.error("Logout error:", error);
          // Still redirect to login even if logout request fails
          this.router.navigate(["/auth/login"]);
        },
      });
  }

  /**
   * Handle search input changes with autocomplete
   * Calls GeographicService to fetch matching communes
   */
  onSearchInput(): void {
    // Clear results if query is too short
    if (!this.searchQuery || this.searchQuery.trim().length < 2) {
      this.searchResults = null;
      return;
    }

    // Search for communes using the geographic service
    this.geographicService
      .searchCommunes(this.searchQuery.trim())
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results) => {
          this.searchResults = results;
          console.log(`Found ${results.length} communes matching "${this.searchQuery}"`);
        },
        error: (error) => {
          console.error("Search error:", error);
          this.searchResults = [];
          this.snackBar.open("Erreur lors de la recherche", "Fermer", { duration: 3000 });
        },
      });
  }

  /**
   * Handle search result selection
   * Maps Commune to CommuneWithAirQuality and selects it on map
   */
  onSearchResultClicked(commune: Commune): void {
    console.log("Search result clicked:", commune.name);

    // Find matching commune with air quality data from loaded communes
    const communeWithAirQuality = this.communes.find((c) => c.inseeCode === commune.inseeCode);

    if (communeWithAirQuality) {
      // Select commune through map service
      this.mapService.selectCommune(communeWithAirQuality);

      // Center map on selected commune with smooth animation
      if (this.mapView) {
        this.mapView.centerOnCommune(communeWithAirQuality);
      }

      // On mobile, open side panel
      if (this.isMobileView) {
        this.isSidePanelOpen = true;
      }

      // Clear search state
      this.searchQuery = "";
      this.searchResults = null;

      this.snackBar.open(`Commune ${commune.name} sélectionnée`, "Fermer", {
        duration: 2000,
      });
    } else {
      console.warn(`Commune ${commune.name} not found in loaded communes with air quality data`);
      this.snackBar.open("Commune trouvée mais données indisponibles", "Fermer", { duration: 3000 });
    }
  }

  /**
   * Clear search results and query
   */
  closeSearchResults(): void {
    this.searchQuery = "";
    this.searchResults = null;
  }

  /**
   * Fetch detailed weather and air quality data for a commune
   * Uses Promise.all() to fetch both services in parallel for better performance
   * Implements AIRSEN's error handling patterns with graceful degradation
   *
   * @param inseeCode 5-digit INSEE code of the commune
   */
  private async fetchDetailedData(inseeCode: string): Promise<void> {
    console.log(`Fetching detailed data for commune: ${inseeCode}`);

    this.isLoadingDetails = true;

    try {
      // Use Promise.all to fetch weather and air quality data in parallel
      // This is more efficient than sequential calls and reduces loading time
      const [weatherResult, airQualityResult] = await Promise.all([
        // Convert observables to promises with error handling
        firstValueFrom(this.weatherService.getCurrentWeather(inseeCode)).catch((error) => {
          console.warn(`Weather service failed for commune ${inseeCode}:`, error);
          return null; // Return null instead of throwing to allow partial data display
        }),

        firstValueFrom(this.airQualityService.getAirLatestQuality(inseeCode)).catch((error) => {
          console.warn(`Air quality service failed for commune ${inseeCode}:`, error);
          return null; // Return null instead of throwing to allow partial data display
        }),
      ]);

      // Store results in component state for template binding
      this.weatherDetails = weatherResult;
      this.airQualityDetails = airQualityResult;

      // Log success for debugging and monitoring
      const dataSourcesLoaded = [];
      if (weatherResult) dataSourcesLoaded.push("weather");
      if (airQualityResult) dataSourcesLoaded.push("air quality");

      console.log(`Detailed data loaded for ${inseeCode}: [${dataSourcesLoaded.join(", ")}]`);

      // Show user notification if both services failed
      if (!weatherResult && !airQualityResult) {
        this.snackBar.open("Données détaillées temporairement indisponibles", "Fermer", { duration: 3000 });
      }
    } catch (error) {
      // This catch should rarely be triggered due to inner catch blocks
      // but provides final safety net for unexpected errors
      console.error(`Unexpected error fetching detailed data for commune ${inseeCode}:`, error);

      // Reset state on unexpected error
      this.weatherDetails = null;
      this.airQualityDetails = null;

      this.snackBar.open("Erreur lors du chargement des données détaillées", "Fermer", { duration: 3000 });
    } finally {
      this.isLoadingDetails = false;
    }
  }
}
