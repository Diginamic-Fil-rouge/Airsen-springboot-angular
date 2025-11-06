import { Component, OnInit, OnDestroy, inject, ViewChild } from "@angular/core";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { MatSnackBar } from "@angular/material/snack-bar";
import { MapService } from "./services/map.service";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";
import { MapFilter, PollutantType, TimeRange, MapStyle } from "./models/map-filter.model";
import { MapViewComponent } from "./components/map-view/map-view.component";
import { ExportDataService } from "@/core/services/export-data.service";
import { PdfGenerationService } from "@/core/services/pdf-generation.service";

/**
 * Main Map Container Component
 * Full-screen air quality map interface for AIRSEN communes
 */
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

  ngOnInit(): void {
    this.checkMobileView();
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
   * Setup subscriptions to service observables
   */
  private setupSubscriptions(): void {
    // Subscribe to selected commune
    this.mapService.selectedCommune$.pipe(takeUntil(this.destroy$)).subscribe((commune) => {
      this.selectedCommune = commune;
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
      await this.pdfGenerationService.generateCommunePDF(this.selectedCommune);
      this.snackBar.open("PDF généré avec succès", "Fermer", { duration: 3000 });
    } catch (error) {
      console.error("Error generating PDF:", error);
      this.snackBar.open("Erreur lors de la génération du PDF", "Fermer", { duration: 3000 });
    } finally {
      this.isExportingPDF = false;
    }
  }
}
