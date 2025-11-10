import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
import { GeographicService } from "@/app/features/map/services/geographic.service";
import { Commune } from "@/shared/models";

/**
 * Service for managing commune data with reactive state management.
 *
 * Provides centralized commune data loading and caching for application startup.
 * Uses BehaviorSubject for reactive state management, allowing components
 * to subscribe to commune data and loading state changes.
 */
@Injectable({
  providedIn: "root",
})
export class CommuneDataService {
  /**
   * BehaviorSubject holding the current communes array.
   * Initialized with empty array and updated when data loads.
   */
  private communesSubject = new BehaviorSubject<Commune[]>([]);

  /**
   * Observable stream of communes for component subscription.
   * Components can subscribe to this to reactively update when commune data changes.
   */
  public communes$ = this.communesSubject.asObservable();

  /**
   * BehaviorSubject tracking loading state.
   * Used to show loading indicators during data fetching.
   */
  private loadingSubject = new BehaviorSubject<boolean>(false);

  /**
   * Observable stream of loading state for component subscription.
   * Components can use this to show/hide loading spinners.
   */
  public loading$ = this.loadingSubject.asObservable();

  constructor(private geographicService: GeographicService) {}

  /**
   * Loads commune data on application startup in a non-blocking manner.
   *
   * This method is designed for Angular's APP_INITIALIZER:
   * - Returns immediately resolved Promise (non-blocking)
   * - Performs data loading in background via subscription
   * - Updates reactive state when data arrives
   * - Handles errors gracefully with logging
   *
   * The non-blocking approach ensures application startup is not delayed
   * by network requests or API failures.
   *
   * @returns Promise<void> that resolves immediately (non-blocking)
   */
  loadCommunesOnStartup(): Promise<void> {
    console.log("Starting commune data loading on application startup");
    this.loadingSubject.next(true);

    // Start background loading
    this.geographicService.getAllCommunesWithCoordinates().subscribe({
      next: (communes) => {
        this.communesSubject.next(communes);
        this.loadingSubject.next(false);
        console.log(`Successfully loaded ${communes.length} communes on startup`);
      },
      error: (error) => {
        console.error("Failed to load communes on startup:", error);
        this.loadingSubject.next(false);
        // Keep empty array as fallback - application can still function
        console.warn("Application will continue with empty commune data - map features may be limited");
      },
    });

    // Return immediately resolved Promise (non-blocking behavior)
    return Promise.resolve();
  }

  /**
   * Gets the current communes from cache synchronously.
   *
   * Useful for components that need immediate access to commune data
   * without subscribing to the observable stream.
   *
   * @returns Commune[] Current communes array (empty if not loaded yet)
   */
  getCommunesFromCache(): Commune[] {
    return this.communesSubject.value;
  }

  /**
   * Gets the current loading state synchronously.
   *
   * @returns boolean Current loading state
   */
  isLoading(): boolean {
    return this.loadingSubject.value;
  }

  /**
   * Manually refresh commune data.
   *
   * Can be used by components to trigger a manual refresh
   * of commune data (e.g., after network reconnection).
   *
   * @returns Promise<void> Promise that resolves when refresh completes
   */
  refreshCommunes(): Promise<void> {
    console.log("Manually refreshing commune data");
    return this.loadCommunesOnStartup();
  }
}
