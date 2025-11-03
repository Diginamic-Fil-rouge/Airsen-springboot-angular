import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '@/environments/environment';
import { ExportDataResponse, HistoricalDataResponse } from '@/shared/models/export.model';

/**
 * Export Data Service
 *
 * Provides HTTP methods to fetch export data from backend API endpoints
 * for PDF and CSV generation from current and historical environmental data.
 *
 * Backend endpoints:
 * - GET /api/v1/communes/{inseeCode}/export-data (current snapshot)
 * - GET /api/v1/communes/{inseeCode}/historical-data (time-series)
 */
@Injectable({
  providedIn: 'root'
})
export class ExportDataService {
  private readonly apiUrl = `${environment.apiUrl}/communes`;

  constructor(private http: HttpClient) {}

  /**
   * Fetch current/snapshot environmental data for a commune
   *
   * @param inseeCode - 5-digit INSEE code (e.g., '75056')
   * @returns Observable<ExportDataResponse> - Current AQI, weather, and population data
   *
   * @example
   * this.exportDataService.getExportData('75056').subscribe({
   *   next: (data) => {
   *     console.log('AQI:', data.airQuality.aqi);
   *     console.log('Temperature:', data.weather.temperature);
   *   },
   *   error: (err) => console.error('Failed to fetch:', err)
   * });
   */
  getExportData(inseeCode: string): Observable<ExportDataResponse> {
    return this.http.get<ExportDataResponse>(
      `${this.apiUrl}/${inseeCode}/export-data`
    ).pipe(
      catchError(error => {
        console.error('Export data fetch failed:', error);
        return throwError(() => new Error('Failed to fetch export data'));
      })
    );
  }

  /**
   * Fetch historical/time-series environmental data for a commune
   *
   * @param inseeCode - 5-digit INSEE code (e.g., '75056')
   * @param startDate - Start date in YYYY-MM-DD format (e.g., '2025-09-01')
   * @param endDate - End date in YYYY-MM-DD format (e.g., '2025-10-09')
   * @returns Observable<HistoricalDataResponse> - Time-series data points with data completeness
   *
   * @example
   * this.exportDataService.getHistoricalData('75056', '2025-09-01', '2025-10-09').subscribe({
   *   next: (data) => {
   *     console.log('Data points:', data.dataPoints.length);
   *     console.log('Completeness:', data.dataCompleteness.completenessPercent + '%');
   *   },
   *   error: (err) => console.error('Failed to fetch:', err)
   * });
   */
  getHistoricalData(
    inseeCode: string,
    startDate: string,
    endDate: string
  ): Observable<HistoricalDataResponse> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<HistoricalDataResponse>(
      `${this.apiUrl}/${inseeCode}/historical-data`,
      { params }
    ).pipe(
      catchError(error => {
        console.error('Historical data fetch failed:', error);
        return throwError(() => new Error('Failed to fetch historical data'));
      })
    );
  }
}
