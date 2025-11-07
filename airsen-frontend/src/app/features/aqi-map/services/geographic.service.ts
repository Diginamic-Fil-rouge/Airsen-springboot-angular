import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable, of } from "rxjs";
import { catchError, debounceTime, distinctUntilChanged } from "rxjs/operators";
import { environment } from "@/environments/environment";
import { Commune } from "@/shared/models/commune.model";

/**
 * Geographic Service
 *
 * Provides HTTP methods to fetch geographic data from backend API endpoints
 * including commune search functionality with autocomplete support.
 *
 * Backend endpoints:
 * - GET /api/v1/communes/search?q={query} (search communes by name/INSEE)
 * - GET /api/v1/communes/{inseeCode}/detail (get detailed commune info)
 * - GET /api/v1/communes/with-coordinates (get communes with coordinates)
 * - GET /api/v1/regions (get all regions)
 * - GET /api/v1/regions/{regionId}/departments (get departments by region)
 * - GET /api/v1/departments (get all departments)
 * - GET /api/v1/departments/{id} (get specific department)
 * - GET /api/v1/departments/{departmentId}/communes (get communes by department)
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
   * Uses backend endpoint: GET /communes/{inseeCode}/detail
   *
   * @param inseeCode 5-digit INSEE code
   * @returns Observable<Commune>
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
   * Get all communes with coordinates (used by map service)
   * Uses backend endpoint: GET /communes/with-coordinates
   */
  getAllCommunesWithCoordinates(): Observable<Commune[]> {
    return this.http.get<Commune[]>(`${this.BASE_URL}/communes/with-coordinates`).pipe(
      catchError((error) => {
        console.error("Failed to fetch communes with coordinates:", error);
        return of([]);
      })
    );
  }

  /**
   * Get all regions
   * Uses backend endpoint: GET /regions
   */
  getAllRegions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.BASE_URL}/regions`).pipe(
      catchError((error) => {
        console.error("Failed to fetch regions:", error);
        return of([]);
      })
    );
  }

  /**
   * Get all departments
   * Uses backend endpoint: GET /departments
   */
  getAllDepartments(): Observable<any[]> {
    return this.http.get<any[]>(`${this.BASE_URL}/departments`).pipe(
      catchError((error) => {
        console.error("Failed to fetch departments:", error);
        return of([]);
      })
    );
  }

  /**
   * Get departments by region
   * Uses backend endpoint: GET /regions/{regionId}/departments
   */
  getDepartmentsByRegion(regionId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.BASE_URL}/regions/${regionId}/departments`).pipe(
      catchError((error) => {
        console.error(`Failed to fetch departments for region ${regionId}:`, error);
        return of([]);
      })
    );
  }

  /**
   * Get communes by department
   * Uses endpoint: GET /departments/{departmentId}/communes
   */
  getCommunesByDepartment(departmentId: string): Observable<Commune[]> {
    return this.http.get<Commune[]>(`${this.BASE_URL}/departments/${departmentId}/communes`).pipe(
      catchError((error) => {
        console.error(`Failed to fetch communes for department ${departmentId}:`, error);
        return of([]);
      })
    );
  }

  /**
   * Get specific department by ID
   * Uses backend endpoint: GET /departments/{id}
   */
  getDepartmentById(id: string): Observable<any> {
    return this.http.get<any>(`${this.BASE_URL}/departments/${id}`).pipe(
      catchError((error) => {
        console.error(`Failed to fetch department ${id}:`, error);
        throw error;
      })
    );
  }
}
