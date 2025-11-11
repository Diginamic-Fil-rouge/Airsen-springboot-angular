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

  /**
   * Fetch detailed commune data including pollutant breakdown.
   *
   * This endpoint returns comprehensive commune information including:
   * - Air quality index (ATMO index, qualifier, color)
   * - Detailed pollutant concentrations (PM2.5, PM10, SO2, NO2, O3)
   * - Geographic and demographic information
   * - Measurement timestamps and data source
   *
   * @param inseeCode INSEE code of the commune (e.g., "75056" for Paris)
   * @returns Observable<CommuneWithAirQuality> Detailed commune data with pollutants
   *
   * @example
   * getCommuneDetail("75056").subscribe(commune => {
   *   console.log(commune.pollutants); // { pm25: 15, pm10: 25, ... }
   * });
   */
  getCommuneDetail(inseeCode: string): Observable<CommuneWithAirQuality> {
    console.log(`[CommuneDataService] Fetching detail for commune ${inseeCode}`);

    return this.http.get<any>(`${environment.apiUrl}/communes/${inseeCode}/detail`).pipe(
      tap((response) => {
        console.log(`[CommuneDataService] Loaded detail for ${response.name}`);
      }),
      map((response) => this.transformDetailResponseToCommuneWithAirQuality(response)),
      catchError((error) => {
        console.error(`[CommuneDataService] Failed to fetch commune detail for ${inseeCode}:`, error);
        throw error;
      })
    );
  }

  /**
   * Transforms backend commune detail response to CommuneWithAirQuality format.
   *
   * Backend returns nested airQuality object with pollutants inside,
   * Frontend expects pollutants at the top level for component usage.
   *
   * @param response Backend API response from /communes/{inseeCode}/detail
   * @returns CommuneWithAirQuality Transformed commune data
   */
  private transformDetailResponseToCommuneWithAirQuality(response: any): CommuneWithAirQuality {
    return {
      id: 0,
      inseeCode: response.inseeCode,
      name: response.name,
      departmentCode: response.departmentCode,
      regionCode: response.regionCode,
      population: response.population,
      latitude: response.latitude,
      longitude: response.longitude,
      currentAirQuality: response.airQuality
        ? {
            atmoIndex: response.airQuality.atmoIndex,
            qualifier: response.airQuality.qualifier,
            color: response.airQuality.color,
          }
        : undefined,
      pollutants: response.airQuality?.pollutants
        ? {
            pm25: response.airQuality.pollutants.pm25,
            pm10: response.airQuality.pollutants.pm10,
            so2: response.airQuality.pollutants.so2,
            no2: response.airQuality.pollutants.no2,
            o3: response.airQuality.pollutants.o3,
          }
        : undefined,
    };
  }
}
