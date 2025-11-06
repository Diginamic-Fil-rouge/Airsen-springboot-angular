import { Injectable, inject } from "@angular/core";
import { Observable, BehaviorSubject, combineLatest, of } from "rxjs";
import { map, catchError, tap } from "rxjs/operators";
import { GeographicService } from "@/features/map/services/geographic.service";
import { AirQualityService } from "@/features/map/services/air-quality.service";
import { FavoriteService } from "@/features/favorites/services/favorite.service";
import { AuthService } from "@/auth/services/auth.service";
import { Commune, CommuneWithAirQuality } from "@/shared/models/commune.model";
import { MapFilter, DEFAULT_MAP_FILTER } from "../models/map-filter.model";

/**
 * Map Service
 * Orchestrates data fetching and transformation for the air quality map
 * Uses AIRSEN's existing commune models and services
 * Backend provides atmoIndex, qualifier, and color - service just passes data through
 */
@Injectable({
  providedIn: "root",
})
export class MapService {
  private geographicService = inject(GeographicService);
  private airQualityService = inject(AirQualityService);
  private favoriteService = inject(FavoriteService);
  private authService = inject(AuthService);

  // State management
  private filterSubject = new BehaviorSubject<MapFilter>(DEFAULT_MAP_FILTER);
  private selectedCommuneSubject = new BehaviorSubject<CommuneWithAirQuality | null>(null);
  private communesSubject = new BehaviorSubject<CommuneWithAirQuality[]>([]);

  public filter$ = this.filterSubject.asObservable();
  public selectedCommune$ = this.selectedCommuneSubject.asObservable();
  public communes$ = this.communesSubject.asObservable();

  /**
   * Load all communes with air quality data
   */
  loadCommunes(): Observable<CommuneWithAirQuality[]> {
    return this.geographicService.getCommunesWithCoordinatesAndMinPop().pipe(
      map((communes) => {
        const communesWithAqi: CommuneWithAirQuality[] = communes.map((commune) =>
          this.mapCommuneWithAirQuality(commune)
        );
        this.communesSubject.next(communesWithAqi);
        return communesWithAqi;
      }),
      catchError((error) => {
        console.error("Error loading communes:", error);
        return of([]);
      })
    );
  }

  /**
   * Get commune details by INSEE code
   */
  getCommuneDetails(inseeCode: string): Observable<CommuneWithAirQuality | null> {
    return this.geographicService.getCommuneDatas(inseeCode).pipe(
      map((data) => {
        if (!data) return null;

        const commune: CommuneWithAirQuality = {
          id: data.id || 0,
          inseeCode: inseeCode,
          name: data.name || "",
          latitude: data.latitude || 0,
          longitude: data.longitude || 0,
          population: data.population,
          departmentCode: data.departmentCode,
          regionCode: data.regionCode,
          currentAirQuality: {
            atmoIndex: data.airQuality?.atmoIndex || 0,
            qualifier: data.airQuality?.qualifier || "Inconnu",
            color: data.airQuality?.color || "#999999",
          },
        };

        this.selectedCommuneSubject.next(commune);
        return commune;
      }),
      catchError((error) => {
        console.error("Error loading commune details:", error);
        return of(null);
      })
    );
  }

  /**
   * Update map filter
   */
  updateFilter(filter: Partial<MapFilter>): void {
    const currentFilter = this.filterSubject.value;
    this.filterSubject.next({ ...currentFilter, ...filter });
  }

  /**
   * Select a commune
   */
  selectCommune(commune: CommuneWithAirQuality | null): void {
    this.selectedCommuneSubject.next(commune);
  }

  /**
   * Get filtered communes based on current filter
   */
  getFilteredCommunes(): Observable<CommuneWithAirQuality[]> {
    return combineLatest([this.communes$, this.filter$]).pipe(
      map(([communes, filter]) => {
        let filtered = communes;

        // Filter by search query
        if (filter.searchQuery) {
          const query = filter.searchQuery.toLowerCase();
          filtered = filtered.filter((c) => c.name.toLowerCase().includes(query) || c.inseeCode.includes(query));
        }

        // Filter by ATMO index levels (backend provides the index)
        if (filter.aqiLevels.length > 0) {
          filtered = filtered.filter((c) => {
            const atmoIndex = c.currentAirQuality?.atmoIndex || 0;
            // Map ATMO index to level string for filtering
            const levelMap: Record<number, string> = {
              1: "GOOD",
              2: "MODERATE",
              3: "UNHEALTHY_SENSITIVE",
              4: "UNHEALTHY",
              5: "VERY_UNHEALTHY",
              6: "HAZARDOUS",
            };
            const level = levelMap[atmoIndex] || "UNKNOWN";
            return filter.aqiLevels.includes(level);
          });
        }

        // Filter by departments
        if (filter.departments.length > 0) {
          filtered = filtered.filter((c) => c.departmentCode && filter.departments.includes(c.departmentCode));
        }

        // Filter by regions
        if (filter.regions.length > 0) {
          filtered = filtered.filter((c) => c.regionCode && filter.regions.includes(c.regionCode));
        }

        return filtered;
      })
    );
  }

  /**
   * Check if user has favorited a commune
   */
  checkFavoriteStatus(inseeCode: string): Observable<boolean> {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) return of(false);

    return this.favoriteService.getUserFavorites(currentUser.id).pipe(
      map((favorites) => favorites.some((f) => f.communeInseeCode === inseeCode)),
      catchError(() => of(false))
    );
  }

  // =========================================================================
  // PRIVATE HELPER METHODS
  // =========================================================================

  /**
   * Map commune data to CommuneWithAirQuality
   * Backend provides atmoIndex, qualifier, and color - just pass through
   */
  private mapCommuneWithAirQuality(commune: any): CommuneWithAirQuality {
    const atmoIndex = commune.currentAirQuality?.atmoIndex || commune.airQuality?.atmoIndex || 0;

    const qualifier = commune.currentAirQuality?.qualifier || commune.airQuality?.qualifier || "Inconnu";

    const color = commune.currentAirQuality?.color || commune.airQuality?.color || "#999999";

    return {
      id: commune.id || 0,
      inseeCode: commune.inseeCode || "",
      name: commune.name || "",
      latitude: commune.latitude || 0,
      longitude: commune.longitude || 0,
      population: commune.population,
      departmentCode: commune.departmentCode,
      regionCode: commune.regionCode,
      currentAirQuality: {
        atmoIndex: atmoIndex,
        qualifier: qualifier,
        color: color,
      },
    };
  }
}
