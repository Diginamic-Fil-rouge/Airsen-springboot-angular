import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { BehaviorSubject, Observable, of } from "rxjs";
import { catchError, tap, map } from "rxjs/operators";
import { Commune, CommuneWithAirQuality, CommuneMapData } from "@/shared/models";
import { environment } from "@/environments/environment";

/**
 * Service for managing commune data with reactive state management and progressive loading.
 *
 * PERFORMANCE OPTIMIZATIONS:
 * - Progressive loading: Loads communes based on population thresholds
 * - Initial load: ~18 communes (pop >= 50K) instead of 10K+ communes
 * - Authentication required: Uses JWT tokens for API access
 * - Caching: Maintains loaded communes to avoid duplicate requests
 *
 * Progressive Loading Strategy:
 * - Zoom 6 (initial): minPopulation=50000 → ~18 communes (~5KB)
 * - Zoom 8-9: minPopulation=20000 → ~68 communes (~15KB)
 * - Zoom 10-11: minPopulation=10000 → ~163 communes (~35KB)
 * - Zoom 12+: no filter → ~10,381 communes (~2MB)
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
   * Observable loading state stream for components.
   */
  public loading$ = this.loadingSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Fetch communes with coordinates, optionally filtered by minimum population.
   *
   * PERFORMANCE OPTIMIZATION: Use minPopulation filter for initial map load to prevent
   * loading 10,000+ communes (~2MB) on page load. Progressive loading strategy loads
   * more communes as user zooms in.
   *
   * Population thresholds for progressive loading:
   * - Zoom 6 (initial): minPopulation=50000 → ~18 communes (~5KB) - Major cities only
   * - Zoom 8-9: minPopulation=20000 → ~68 communes (~15KB)
   * - Zoom 10-11: minPopulation=10000 → ~163 communes (~35KB)
   * - Zoom 12+: minPopulation=undefined → All 10K+ communes (~2MB)
   *
   * @param minPopulation Optional minimum population filter (undefined = all communes)
   * @returns Observable<CommuneWithAirQuality[]> Communes with air quality data
   *
   * @example
   * // Initial load (zoom 6): Major cities only
   * getAllCommunesWithCoordinates(50000).subscribe(...)
   *
   * // Zoom 10: Medium-sized communes
   * getAllCommunesWithCoordinates(10000).subscribe(...)
   *
   * // Zoom 12+: All communes
   * getAllCommunesWithCoordinates().subscribe(...)
   */
  getAllCommunesWithCoordinates(minPopulation?: number): Observable<CommuneWithAirQuality[]> {
    this.loadingSubject.next(true);

    let params = new HttpParams();
    if (minPopulation !== undefined) {
      params = params.set("minPopulation", minPopulation.toString());
    }

    const filterMsg = minPopulation ? `(pop >= ${minPopulation})` : "(all)";
    console.log(`[CommuneDataService] Fetching communes ${filterMsg}`);

    return this.http.get<CommuneMapData[]>(`${environment.apiUrl}/communes/with-coordinates`, { params }).pipe(
      tap((communeMapData) => {
        console.log(`[CommuneDataService] Loaded ${communeMapData.length} communes ${filterMsg}`);
        this.loadingSubject.next(false);
      }),
      // Transform backend format to frontend format
      map((communeMapData) => this.transformMapDataToCommunesWithAirQuality(communeMapData)),
      catchError((error) => {
        console.error("[CommuneDataService] Failed to fetch communes:", error);
        this.loadingSubject.next(false);
        return of([]);
      })
    );
  }

  /**
   * Transforms backend CommuneMapData format to frontend CommuneWithAirQuality format.
   *
   * Backend returns air quality data as flat properties (atmoIndex, qualifier, color),
   * Frontend expects nested currentAirQuality object for better component usage.
   *
   * @param mapData Array of communes in backend format
   * @returns Array of communes in frontend format
   */
  private transformMapDataToCommunesWithAirQuality(mapData: CommuneMapData[]): CommuneWithAirQuality[] {
    return mapData.map((commune) => ({
      id: commune.id,
      inseeCode: commune.inseeCode,
      name: commune.name,
      departmentCode: commune.departmentCode,
      regionCode: commune.regionCode,
      population: commune.population,
      latitude: commune.latitude,
      longitude: commune.longitude,
      currentAirQuality:
        commune.atmoIndex !== null && commune.qualifier !== null && commune.color !== null
          ? {
              atmoIndex: commune.atmoIndex,
              qualifier: commune.qualifier,
              color: commune.color,
            }
          : undefined,
    }));
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
}
