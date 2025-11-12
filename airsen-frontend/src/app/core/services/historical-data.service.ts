import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "@/environments/environment";

export interface HistoricalDataResponse {
  commune: {
    name: string;
    inseeCode: string;
  };
  dateRange: {
    start: string;
    end: string;
  };
  dataPoints: DataPoint[];
  summary: {
    totalDataPoints: number;
    completeness: {
      airQuality: number;
      weather: number;
    };
  };
}

export interface DataPoint {
  timestamp: string;
  airQuality: {
    aqi: number;
    no2?: number;
    o3?: number;
    pm10?: number;
    pm25?: number;
    so2?: number;
  } | null;
  weather: {
    temperature: number;
    humidity: number;
    windSpeed: number;
  } | null;
}

/**
 * Service for fetching historical air quality and weather data.
 *
 * Provides access to 24-hour trend data for communes, enabling
 * visualization of AQI and temperature patterns over time.
 *
 * Backend Integration:
 * - Endpoint: GET /api/v1/communes/{inseeCode}/historical-data
 * - Query params: startDate (YYYY-MM-DD), endDate (YYYY-MM-DD)
 * - Returns: Hourly data points with air quality and weather metrics
 */
@Injectable({
  providedIn: "root",
})
export class HistoricalDataService {
  constructor(private http: HttpClient) {}

  /**
   * Fetches historical air quality and weather data for a commune.
   *
   * Typical use case: Load 24-hour trend data for chart visualization
   *
   * Date Range Calculation:
   * - For 24h trend: endDate = now, startDate = now - 24 hours
   * - For weekly trend: endDate = now, startDate = now - 7 days
   *
   * @param inseeCode INSEE code of the commune (e.g., "75056" for Paris)
   * @param startDate Start date in YYYY-MM-DD format
   * @param endDate End date in YYYY-MM-DD format
   * @returns Observable<HistoricalDataResponse> Historical data with hourly data points
   *
   * @example
   * // Load 24-hour trend
   * const endDate = new Date();
   * const startDate = new Date(endDate.getTime() - 24 * 60 * 60 * 1000);
   * getHistoricalData("75056", formatDate(startDate), formatDate(endDate))
   *   .subscribe(data => console.log(data.dataPoints));
   */
  getHistoricalData(inseeCode: string, startDate: string, endDate: string): Observable<HistoricalDataResponse> {
    const params = new HttpParams().set("startDate", startDate).set("endDate", endDate);

    console.log(`[HistoricalDataService] Fetching data for ${inseeCode} from ${startDate} to ${endDate}`);

    return this.http.get<HistoricalDataResponse>(`${environment.apiUrl}/communes/${inseeCode}/historical-data`, {
      params,
    });
  }
}
