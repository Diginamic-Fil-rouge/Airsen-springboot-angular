import { Injectable, inject } from "@angular/core";
import { Observable, BehaviorSubject, of } from "rxjs";
import { map, catchError, tap } from "rxjs/operators";
import { GeographicService } from "./geographic.service";
import { AirQualityService } from "./air-quality.service";
import { FavoriteService } from "@/features/favorites/services/favorite.service";
import { AuthService } from "@/auth/services/auth.service";
import { Commune, CommuneWithAirQuality } from "@/shared/models/commune.model";

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
  private selectedCommuneSubject = new BehaviorSubject<CommuneWithAirQuality | null>(null);
  private communesSubject = new BehaviorSubject<CommuneWithAirQuality[]>([]);

  public selectedCommune$ = this.selectedCommuneSubject.asObservable();
  public communes$ = this.communesSubject.asObservable();

  /**
   * Load all communes with air quality data
   */
  loadCommunes(): Observable<CommuneWithAirQuality[]> {
    return this.geographicService.getAllCommunesWithCoordinates().pipe(
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
    return this.geographicService.getCommuneDetails(inseeCode).pipe(
      map((data) => {
        if (!data) return null;

        const commune: CommuneWithAirQuality = {
          id: 0,
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
          pollutants: {
            pm25: data.airQuality?.pollutants?.pm25,
            pm10: data.airQuality?.pollutants?.pm10,
            o3: data.airQuality?.pollutants?.o3,
            no2: data.airQuality?.pollutants?.no2,
            so2: data.airQuality?.pollutants?.so2,
            co: (data as any)?.airQuality?.pollutants?.co,
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
   * Update map filter (used for mapStyle changes)
   */
  updateFilter(filter: Partial<any>): void {
    // Simplified implementation - only handles mapStyle now
    // Filter state management removed as part of UI redesign
  }

  /**
   * Select a commune
   */
  selectCommune(commune: CommuneWithAirQuality | null): void {
    this.selectedCommuneSubject.next(commune);
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
