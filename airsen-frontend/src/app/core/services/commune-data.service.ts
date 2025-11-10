import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
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
}
