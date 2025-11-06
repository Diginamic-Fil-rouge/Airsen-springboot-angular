import { Component, OnInit, OnDestroy, inject, ViewChild } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MapService } from './services/map.service';
import { Station } from './models/station.model';
import { MapFilter, PollutantType, TimeRange, MapStyle } from './models/map-filter.model';
import { MapViewComponent } from './components/map-view/map-view.component';

/**
 * Main Map Container Component
 * Full-screen air quality map interface similar to aqi.in
 */
@Component({
  standalone: false,
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit, OnDestroy {
  private mapService = inject(MapService);
  private destroy$ = new Subject<void>();

  @ViewChild('mapView') mapView!: MapViewComponent;

  // Component State
  isLoading = true;
  isSidePanelOpen = true;
  isMobileView = false;
  selectedCommune: Station | null = null;
  communes: Station[] = [];
  currentFilter: MapFilter | null = null;

  ngOnInit(): void {
    this.checkMobileView();
    this.setupSubscriptions();
    this.loadStations();

    // Listen for window resize
    window.addEventListener('resize', () => this.checkMobileView());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    window.removeEventListener('resize', () => this.checkMobileView());
  }

  /**
   * Setup subscriptions to service observables
   */
  private setupSubscriptions(): void {
    // Subscribe to selected commune
    this.mapService.selectedStation$
      .pipe(takeUntil(this.destroy$))
      .subscribe(commune => {
        this.selectedCommune = commune;
      });

    // Subscribe to communes
    this.mapService.stations$
      .pipe(takeUntil(this.destroy$))
      .subscribe(communes => {
        this.communes = communes;
      });

    // Subscribe to filter changes
    this.mapService.filter$
      .pipe(takeUntil(this.destroy$))
      .subscribe(filter => {
        this.currentFilter = filter;
      });
  }

  /**
   * Load all communes
   */
  private loadStations(): void {
    this.isLoading = true;
    this.mapService.loadStations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading communes:', error);
          this.isLoading = false;
        }
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
  onCommuneSelected(commune: Station): void {
    this.mapService.selectStation(commune);

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
    this.mapService.selectStation(null);
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
}
