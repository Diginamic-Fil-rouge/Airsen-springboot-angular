import { Injectable, inject } from '@angular/core';
import { Observable, BehaviorSubject, combineLatest, of } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { GeographicService } from '@/features/map/services/geographic.service';
import { AirQualityService } from '@/features/map/services/air-quality.service';
import { FavoriteService } from '@/features/favorites/services/favorite.service';
import { AuthService } from '@/auth/services/auth.service';
import {
  Station,
  AqiLevel,
  getAqiColor,
  getAqiLevelFromValue,
  PollutantData
} from '../models/station.model';
import { MapFilter, DEFAULT_MAP_FILTER } from '../models/map-filter.model';

/**
 * Map Service
 * Orchestrates data fetching and transformation for the air quality map
 */
@Injectable({
  providedIn: 'root'
})
export class MapService {
  private geographicService = inject(GeographicService);
  private airQualityService = inject(AirQualityService);
  private favoriteService = inject(FavoriteService);
  private authService = inject(AuthService);

  // State management
  private filterSubject = new BehaviorSubject<MapFilter>(DEFAULT_MAP_FILTER);
  private selectedStationSubject = new BehaviorSubject<Station | null>(null);
  private stationsSubject = new BehaviorSubject<Station[]>([]);

  public filter$ = this.filterSubject.asObservable();
  public selectedStation$ = this.selectedStationSubject.asObservable();
  public stations$ = this.stationsSubject.asObservable();

  /**
   * Load all stations with air quality data
   */
  loadStations(): Observable<Station[]> {
    return this.geographicService.getCommunesWithCoordinatesAndMinPop().pipe(
      map(communes => {
        const stations: Station[] = communes.map(commune => this.mapCommuneToStation(commune));
        this.stationsSubject.next(stations);
        return stations;
      }),
      catchError(error => {
        console.error('Error loading stations:', error);
        return of([]);
      })
    );
  }

  /**
   * Get station details by INSEE code
   */
  getStationDetails(inseeCode: string): Observable<Station | null> {
    return this.geographicService.getCommuneDatas(inseeCode).pipe(
      map(data => {
        if (!data) return null;

        const station: Station = {
          id: data.id || 0,
          inseeCode: inseeCode,
          name: data.name || '',
          latitude: data.latitude || 0,
          longitude: data.longitude || 0,
          currentAqi: data.airQuality?.atmoIndex || 0,
          aqiLevel: this.getAqiLevelFromAtmoIndex(data.airQuality?.atmoIndex || 0),
          aqiColor: getAqiColor(this.getAqiLevelFromAtmoIndex(data.airQuality?.atmoIndex || 0)),
          lastUpdated: data.airQuality?.lastUpdated || new Date(),
          pollutants: this.extractPollutants(data.airQuality),
          population: data.population,
          stationType: 'GOVERNMENT' as any,
          isFavorite: false
        };

        this.selectedStationSubject.next(station);
        return station;
      }),
      catchError(error => {
        console.error('Error loading station details:', error);
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
   * Select a station
   */
  selectStation(station: Station | null): void {
    this.selectedStationSubject.next(station);
  }

  /**
   * Get filtered stations based on current filter
   */
  getFilteredStations(): Observable<Station[]> {
    return combineLatest([this.stations$, this.filter$]).pipe(
      map(([stations, filter]) => {
        let filtered = stations;

        // Filter by search query
        if (filter.searchQuery) {
          const query = filter.searchQuery.toLowerCase();
          filtered = filtered.filter(s =>
            s.name.toLowerCase().includes(query) ||
            s.inseeCode.includes(query)
          );
        }

        // Filter by AQI levels
        if (filter.aqiLevels.length > 0) {
          filtered = filtered.filter(s =>
            filter.aqiLevels.includes(s.aqiLevel)
          );
        }

        // Filter by station types
        if (filter.stationTypes.length > 0) {
          filtered = filtered.filter(s =>
            filter.stationTypes.includes(s.stationType)
          );
        }

        return filtered;
      })
    );
  }

  /**
   * Check if user has favorited a station
   */
  checkFavoriteStatus(inseeCode: string): Observable<boolean> {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) return of(false);

    return this.favoriteService.getUserFavorites(currentUser.id).pipe(
      map(favorites => favorites.some(f => f.communeInseeCode === inseeCode)),
      catchError(() => of(false))
    );
  }

  // =========================================================================
  // PRIVATE HELPER METHODS
  // =========================================================================

  /**
   * Map commune data to Station
   */
  private mapCommuneToStation(commune: any): Station {
    const atmoIndex = commune.currentAirQuality?.atmoIndex ||
                      commune.airQuality?.atmoIndex ||
                      0;

    return {
      id: commune.id || 0,
      inseeCode: commune.inseeCode || '',
      name: commune.name || '',
      latitude: commune.latitude || 0,
      longitude: commune.longitude || 0,
      currentAqi: atmoIndex,
      aqiLevel: this.getAqiLevelFromAtmoIndex(atmoIndex),
      aqiColor: getAqiColor(this.getAqiLevelFromAtmoIndex(atmoIndex)),
      lastUpdated: new Date(),
      pollutants: {},
      population: commune.population,
      stationType: 'GOVERNMENT' as any,
      isFavorite: false
    };
  }

  /**
   * Extract pollutant data from air quality response
   */
  private extractPollutants(airQuality: any): Station['pollutants'] {
    if (!airQuality) return {};

    return {
      pm25: airQuality.pm25 ? {
        value: airQuality.pm25,
        level: this.getPollutantLevel(airQuality.pm25, 'pm25'),
        unit: 'µg/m³',
        lastUpdated: airQuality.lastUpdated || new Date()
      } : undefined,
      pm10: airQuality.pm10 ? {
        value: airQuality.pm10,
        level: this.getPollutantLevel(airQuality.pm10, 'pm10'),
        unit: 'µg/m³',
        lastUpdated: airQuality.lastUpdated || new Date()
      } : undefined,
      o3: airQuality.o3 ? {
        value: airQuality.o3,
        level: this.getPollutantLevel(airQuality.o3, 'o3'),
        unit: 'µg/m³',
        lastUpdated: airQuality.lastUpdated || new Date()
      } : undefined,
      no2: airQuality.no2 ? {
        value: airQuality.no2,
        level: this.getPollutantLevel(airQuality.no2, 'no2'),
        unit: 'µg/m³',
        lastUpdated: airQuality.lastUpdated || new Date()
      } : undefined,
      so2: airQuality.so2 ? {
        value: airQuality.so2,
        level: this.getPollutantLevel(airQuality.so2, 'so2'),
        unit: 'µg/m³',
        lastUpdated: airQuality.lastUpdated || new Date()
      } : undefined
    };
  }

  /**
   * Convert ATMO index (1-6) to AqiLevel
   */
  private getAqiLevelFromAtmoIndex(atmoIndex: number): AqiLevel {
    switch (atmoIndex) {
      case 1: return AqiLevel.GOOD;
      case 2: return AqiLevel.MODERATE;
      case 3: return AqiLevel.UNHEALTHY_SENSITIVE;
      case 4: return AqiLevel.UNHEALTHY;
      case 5: return AqiLevel.VERY_UNHEALTHY;
      case 6: return AqiLevel.HAZARDOUS;
      default: return AqiLevel.UNKNOWN;
    }
  }

  /**
   * Get pollutant level based on value and type
   */
  private getPollutantLevel(value: number, type: string): AqiLevel {
    // Simplified thresholds - adjust based on WHO/EU standards
    const thresholds: Record<string, number[]> = {
      pm25: [25, 50, 75, 100, 150],
      pm10: [50, 100, 150, 200, 300],
      o3: [100, 160, 240, 380, 800],
      no2: [40, 90, 120, 230, 340],
      so2: [100, 200, 350, 500, 750]
    };

    const limits = thresholds[type] || [50, 100, 150, 200, 300];

    if (value <= limits[0]) return AqiLevel.GOOD;
    if (value <= limits[1]) return AqiLevel.MODERATE;
    if (value <= limits[2]) return AqiLevel.UNHEALTHY_SENSITIVE;
    if (value <= limits[3]) return AqiLevel.UNHEALTHY;
    if (value <= limits[4]) return AqiLevel.VERY_UNHEALTHY;
    return AqiLevel.HAZARDOUS;
  }
}
