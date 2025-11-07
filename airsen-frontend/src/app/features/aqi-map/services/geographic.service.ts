import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable, of } from "rxjs";
import { catchError, debounceTime, distinctUntilChanged } from "rxjs/operators";
import { environment } from "@/environments/environment";
import { Commune, CommuneDatas } from "@/shared/models/commune.model";

/**
 * Geographic Service
 *
 * Provides HTTP methods to fetch geographic data from backend API endpoints
 * including commune search functionality with autocomplete support.
 *
 * Backend endpoints used:
 * - GET /api/v1/communes/search?q={query} (search communes by name/INSEE)
 * - GET /api/v1/communes/{inseeCode}/detail (get detailed commune info)
 * - GET /api/v1/communes/with-coordinates (get communes with coordinates)
 */
@Injectable({
  providedIn: "root",
})
export class GeographicService {
  private readonly BASE_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * Search communes by name with autocomplete support
   * Includes debouncing to prevent excessive API calls
   *
   * @param query Search query string (minimum 2 characters)
   * @returns Observable<Commune[]> - Array of matching communes
   */
  searchCommunes(query: string): Observable<Commune[]> {
    // Return empty array for short queries
    if (!query || query.trim().length < 2) {
      return of([]);
    }

    const params = new HttpParams().set("q", query.trim());

    return this.http.get<Commune[]>(`${this.BASE_URL}/communes/search`, { params }).pipe(
      catchError((error) => {
        console.error("Search communes error:", error);
        // Return empty array on error to prevent UI crashes
        return of([]);
      })
    );
  }

  /**
   * Get detailed information for a specific commune
   */
  getCommuneDetails(inseeCode: string): Observable<Commune> {
    return this.http.get<Commune>(`${this.BASE_URL}/communes/${inseeCode}/detail`).pipe(
      catchError((error) => {
        console.error(`Failed to fetch commune details ${inseeCode}:`, error);
        throw error;
      })
    );
  }

  /**
   * Get detailed commune data (including air quality snapshot) used by map service
   */
  getCommuneDatas(inseeCode: string): Observable<CommuneDatas> {
    return this.http.get<CommuneDatas>(`${this.BASE_URL}/communes/${inseeCode}/detail`).pipe(
      catchError((error) => {
        console.error(`Failed to fetch commune data ${inseeCode}:`, error);
        throw error;
      })
    );
  }

  /**
   * Get all communes with coordinates (used by map service)
   */
  getAllCommunesWithCoordinates(): Observable<Commune[]> {
    return this.http.get<Commune[]>(`${this.BASE_URL}/communes/with-coordinates`).pipe(
      catchError((error) => {
        console.error("Failed to fetch communes with coordinates:", error);
        return of([]);
      })
    );
  }

}
