import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AqiMapService } from './services/aqi-map.service';
import { AqiStation } from './models/aqi-station.model';
import { MapFilter } from './models/map-filter.model';

/**
 * Main AQI Map Container Component
 * Full-screen air quality map interface similar to aqi.in
 */
@Component({
  standalone: false,
  selector: 'app-aqi-map',
  templateUrl: './aqi-map.component.html',
  styleUrls: ['./aqi-map.component.scss']
})
export class AqiMapComponent implements OnInit, OnDestroy {
  private aqiMapService = inject(AqiMapService);
  private destroy$ = new Subject<void>();

  // Component State
  isLoading = true;
  isSidePanelOpen = true;
  isMobileView = false;
  selectedStation: AqiStation | null = null;
  stations: AqiStation[] = [];
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
    // Subscribe to selected station
    this.aqiMapService.selectedStation$
      .pipe(takeUntil(this.destroy$))
      .subscribe(station => {
        this.selectedStation = station;
      });

    // Subscribe to stations
    this.aqiMapService.stations$
      .pipe(takeUntil(this.destroy$))
      .subscribe(stations => {
        this.stations = stations;
      });

    // Subscribe to filter changes
    this.aqiMapService.filter$
      .pipe(takeUntil(this.destroy$))
      .subscribe(filter => {
        this.currentFilter = filter;
      });
  }

  /**
   * Load all stations
   */
  private loadStations(): void {
    this.isLoading = true;
    this.aqiMapService.loadStations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading stations:', error);
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
   * Handle station selection from map
   */
  onStationSelected(station: AqiStation): void {
    this.aqiMapService.selectStation(station);

    // On mobile, open bottom sheet with details
    if (this.isMobileView) {
      this.isSidePanelOpen = true;
    }
  }

  /**
   * Handle filter changes from side panel
   */
  onFilterChanged(filter: Partial<MapFilter>): void {
    this.aqiMapService.updateFilter(filter);
  }

  /**
   * Close station details
   */
  closeStationDetails(): void {
    this.aqiMapService.selectStation(null);
  }
}
