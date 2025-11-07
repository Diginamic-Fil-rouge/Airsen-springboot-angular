import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "@/environments/environment";

/**
 * Weather Service
 *
 * Provides HTTP methods to fetch weather data from backend API endpoints
 * including current weather conditions with AIRSEN's geodistance fallback system.
 *
 * Backend endpoint:
 * - GET /api/v1/weather/current/{inseeCode} (detailed weather data)
 *
 * Data Sources (from backend):
 * - DIRECT: Measured data for the requested commune
 * - ESTIMATED: Estimated from nearest commune within 20km radius
 */
@Injectable({
  providedIn: "root",
})
export class WeatherService {
  private readonly BASE_URL = `${environment.apiUrl}/weather`;

  constructor(private http: HttpClient) {}

  /**
   * Get current detailed weather data for a commune
   * Returns backend WeatherResponse with DataSource transparency (DIRECT/ESTIMATED)
   *
   * @param inseeCode 5-digit INSEE code
   * @returns Observable<any> - Detailed weather response from backend
   */
  getCurrentWeather(inseeCode: string): Observable<any> {
    return this.http.get(`${this.BASE_URL}/current/${inseeCode}`);
  }
}
