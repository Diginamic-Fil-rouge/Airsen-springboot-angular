import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { CommuneDataService } from "@/core/services/commune-data.service";

/**
 * Service interface for air quality data
 *
 * This interface represents the expected structure for air quality responses
 * used by components like AirQualityWidgetComponent.
 */
export interface AirQualityResponse {
  globalIndex?: number;
  aqi?: number;
  globalQuality?: string;
  aqiLabel?: string;
  qualifier?: string;
  color?: string;
  commune: string;
  timestamp: Date;
  measurementDate?: Date;
}

/**
 * Air Quality Service
 *
 * Facade service that provides air quality specific methods by wrapping
 * the CommuneDataService. This service exists to provide a cleaner API
 * for components that only need air quality data without the full commune
 * information.
 *
 * @example
 * // In a component
 * this.airQualityService.getAirLatestQuality('75056')
 *   .subscribe(data => {
 *     console.log('AQI:', data.globalIndex);
 *     console.log('Quality:', data.globalQuality);
 *   });
 */
@Injectable({
  providedIn: "root",
})
export class AirQualityService {
  constructor(private communeDataService: CommuneDataService) {}

  /**
   * Get the latest air quality data for a specific commune
   *
   * @param inseeCode The INSEE code of the commune (e.g., "75056" for Paris)
   * @returns Observable<AirQualityResponse> Latest air quality information
   */
  getAirLatestQuality(inseeCode: string): Observable<AirQualityResponse> {
    return this.communeDataService.getCommuneDetail(inseeCode).pipe(
      map((commune) => {
        const airQuality = commune.currentAirQuality;

        return {
          globalIndex: airQuality?.atmoIndex,
          aqi: airQuality?.atmoIndex,
          globalQuality: airQuality?.qualifier,
          aqiLabel: airQuality?.qualifier,
          qualifier: airQuality?.qualifier,
          color: airQuality?.color,
          commune: commune.name,
          timestamp: new Date(),
          measurementDate: new Date(),
        };
      })
    );
  }

  /**
   * Get air quality data with pollutant breakdown for a specific commune
   *
   * @param inseeCode The INSEE code of the commune
   * @returns Observable with detailed air quality and pollutant data
   */
  getAirQualityWithPollutants(inseeCode: string): Observable<any> {
    return this.communeDataService.getCommuneDetail(inseeCode).pipe(
      map((commune) => {
        const airQuality = commune.currentAirQuality;
        const pollutants = commune.pollutants;

        return {
          globalIndex: airQuality?.atmoIndex,
          globalQuality: airQuality?.qualifier,
          color: airQuality?.color,
          commune: commune.name,
          inseeCode: commune.inseeCode,
          timestamp: new Date(),
          pollutants: pollutants || null,
        };
      })
    );
  }
}
