import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "@/environments/environment";

/**
 * Air Quality Service
 *
 * Provides HTTP methods to fetch air quality data from backend API endpoints
 * including current air quality measurements with AIRSEN's geodistance fallback system.
 *
 * Backend endpoints:
 * - GET /api/v1/atmo/air-quality/{inseeCode} (current air quality data)
 *
 * Data Sources (from backend):
 * - DIRECT: Measured data for the requested commune
 * - ESTIMATED: Estimated from nearest commune within 20km radius
 */
@Injectable({
  providedIn: "root",
})
export class AirQualityService {
  private readonly BASE_URL = `${environment.apiUrl}/atmo`;

  constructor(private http: HttpClient) {}

  /**
   * Get current air quality data for a commune
   * May trigger fresh data fetch if stored data is outdated
   *
   * @param inseeCode 5-digit INSEE code
   * @returns Observable<any> - Current air quality response from backend
   */
  getAirQuality(inseeCode: string): Observable<any> {
    return this.http.get(`${this.BASE_URL}/air-quality/${inseeCode}`);
  }
}
