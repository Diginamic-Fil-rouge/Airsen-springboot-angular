import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError, from, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '@/environments/environment';
import {
  ExportDataResponse,
  HistoricalDataResponse,
  ExportRecord,
  ExportType,
  ExportFormat
} from '@/shared/models/export.model';
import jsPDF from 'jspdf';
import * as Papa from 'papaparse';

/**
 * Export Data Service
 *
 * Client-side PDF/CSV generation service for AIRSEN application:
 * - Fetches aggregated data from backend API endpoints
 * - Generates PDF documents with jsPDF (current snapshot data)
 * - Generates CSV files with PapaParse (historical time-series data)
 * - Manages export history in localStorage (max 50 records)
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
  private readonly STORAGE_KEY = 'airsen_export_history';
  private readonly MAX_HISTORY_RECORDS = 50;

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

  /**
   * Export current data as PDF
   *
   * Flow:
   * 1. Fetch ExportDataResponse from backend
   * 2. Generate PDF with jsPDF using snapshot data
   * 3. Save export record to localStorage history
   * 4. Auto-download PDF file
   *
   * @param inseeCode - 5-digit INSEE code
   * @param locationName - Commune name for display
   * @returns Observable<ExportRecord> - Export metadata
   */
  exportAsPDF(inseeCode: string, locationName: string): Observable<ExportRecord> {
    return this.getExportData(inseeCode).pipe(
      map((data: ExportDataResponse) => {
        const exportId = `exp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        const fileName = `airsen_snapshot_${data.commune.name}_${new Date().toISOString().split('T')[0]}.pdf`;

        const fileSize = this.generatePDF(data, fileName);

        const exportRecord: ExportRecord = {
          id: exportId,
          userId: 0, // Will be set by component if needed
          locationName: data.commune.name,
          inseeCode: data.commune.inseeCode,
          exportType: ExportType.COMBINED,
          format: ExportFormat.PDF,
          fileSize: fileSize,
          createdAt: new Date()
        };

        this.saveExportToHistory(exportRecord);
        return exportRecord;
      })
    );
  }

  /**
   * Export historical data as CSV
   *
   * Flow:
   * 1. Fetch HistoricalDataResponse from backend
   * 2. Generate CSV with PapaParse using time-series data
   * 3. Save export record to localStorage history
   * 4. Auto-download CSV file
   *
   * @param inseeCode - 5-digit INSEE code
   * @param locationName - Commune name for display
   * @param startDate - Start date in YYYY-MM-DD format
   * @param endDate - End date in YYYY-MM-DD format
   * @returns Observable<ExportRecord> - Export metadata
   */
  exportAsCSV(
    inseeCode: string,
    locationName: string,
    startDate: string,
    endDate: string
  ): Observable<ExportRecord> {
    return this.getHistoricalData(inseeCode, startDate, endDate).pipe(
      map((data: HistoricalDataResponse) => {
        const exportId = `exp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        const fileName = `airsen_historical_${data.commune.name}_${startDate}_${endDate}.csv`;

        const fileSize = this.generateCSV(data, fileName);

        const exportRecord: ExportRecord = {
          id: exportId,
          userId: 0,
          locationName: data.commune.name,
          inseeCode: data.commune.inseeCode,
          exportType: ExportType.COMBINED,
          format: ExportFormat.CSV,
          fileSize: fileSize,
          createdAt: new Date()
        };

        this.saveExportToHistory(exportRecord);
        return exportRecord;
      })
    );
  }

  /**
   * Generates PDF document with jsPDF from current snapshot data.
   *
   * PDF Structure:
   * - Header: "AIRSEN - Environmental Data Export"
   * - Location: Commune name, INSEE code, department, region, population
   * - Air Quality Section:
   *   - ATMO Index with color coding (1=green, 6=red)
   *   - Pollutants: NO2, O3, PM10, PM2.5, SO2
   *   - Measurement date
   * - Weather Section:
   *   - Temperature, Humidity, Wind Speed, Direction
   *   - Weather code
   *   - Measurement date
   * - Data Quality Section:
   *   - Freshness indicators
   *   - Data source (cache/API)
   *   - Cache age
   * - Footer: Generated timestamp
   *
   * @param data - ExportDataResponse from backend (current snapshot)
   * @param fileName - Output PDF file name
   * @returns number - Estimated file size in bytes
   */
  private generatePDF(data: ExportDataResponse, fileName: string): number {
    const doc = new jsPDF();
    let yPosition = 20;

    // Header
    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text('AIRSEN - Environmental Data Export', 105, yPosition, { align: 'center' });
    yPosition += 15;

    // Location Information
    doc.setFontSize(12);
    doc.setFont('helvetica', 'normal');
    doc.text(`Location: ${data.commune.name} (INSEE: ${data.commune.inseeCode})`, 20, yPosition);
    yPosition += 6;
    doc.text(`Department: ${data.commune.departmentName}, Region: ${data.commune.regionName}`, 20, yPosition);
    yPosition += 6;
    doc.text(`Population: ${data.commune.population.toLocaleString()}`, 20, yPosition);
    yPosition += 6;
    doc.text(`Coordinates: ${data.commune.latitude.toFixed(4)}, ${data.commune.longitude.toFixed(4)}`, 20, yPosition);
    yPosition += 15;

    // Air Quality Section
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Air Quality Data', 20, yPosition);
    yPosition += 10;

    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');

    // ATMO Index with color coding
    const indexColor = this.getAQIColor(data.airQuality.aqi);
    doc.setTextColor(indexColor.r, indexColor.g, indexColor.b);
    doc.text(`ATMO Index: ${data.airQuality.aqi}`, 25, yPosition);
    doc.setTextColor(0, 0, 0);
    yPosition += 6;

    // Pollutants
    doc.text(`NO2: ${data.airQuality.no2 || 'N/A'} μg/m³`, 25, yPosition);
    yPosition += 6;
    doc.text(`O3: ${data.airQuality.o3 || 'N/A'} μg/m³`, 25, yPosition);
    yPosition += 6;
    doc.text(`PM10: ${data.airQuality.pm10 || 'N/A'} μg/m³`, 25, yPosition);
    yPosition += 6;
    doc.text(`PM2.5: ${data.airQuality.pm25 || 'N/A'} μg/m³`, 25, yPosition);
    yPosition += 6;
    doc.text(`SO2: ${data.airQuality.so2 || 'N/A'} μg/m³`, 25, yPosition);
    yPosition += 6;
    doc.setFontSize(9);
    doc.setTextColor(100, 100, 100);
    doc.text(`Measured: ${data.airQuality.measurementDate}`, 25, yPosition);
    doc.setTextColor(0, 0, 0);
    yPosition += 15;

    // Weather Section
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Weather Data', 20, yPosition);
    yPosition += 10;

    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    doc.text(`Temperature: ${data.weather.temperature}°C`, 25, yPosition);
    yPosition += 6;
    doc.text(`Humidity: ${data.weather.humidity}%`, 25, yPosition);
    yPosition += 6;
    doc.text(`Wind Speed: ${data.weather.windSpeed} km/h`, 25, yPosition);
    yPosition += 6;
    doc.text(`Wind Direction: ${data.weather.windDirection}°`, 25, yPosition);
    yPosition += 6;
    doc.text(`Weather Code: ${data.weather.weatherCode}`, 25, yPosition);
    yPosition += 6;
    doc.setFontSize(9);
    doc.setTextColor(100, 100, 100);
    doc.text(`Measured: ${data.weather.measurementDate}`, 25, yPosition);
    doc.setTextColor(0, 0, 0);
    yPosition += 15;

    // Data Quality Section
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Data Quality', 20, yPosition);
    yPosition += 10;

    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    doc.text(`Air Quality Freshness: ${data.dataQuality.airQualityFreshness}`, 25, yPosition);
    yPosition += 6;
    doc.text(`Weather Freshness: ${data.dataQuality.weatherFreshness}`, 25, yPosition);
    yPosition += 6;
    doc.text(`Data Source: ${data.dataQuality.dataSource}`, 25, yPosition);
    yPosition += 6;
    doc.text(`Cache Age: ${data.dataQuality.cacheAge}`, 25, yPosition);
    yPosition += 6;
    doc.text(`Cache Freshness: ${data.dataQuality.cacheFreshness}%`, 25, yPosition);

    // Footer
    doc.setFontSize(9);
    doc.setTextColor(128, 128, 128);
    doc.text(`Generated: ${new Date().toLocaleString()}`, 105, 285, { align: 'center' });

    // Save and download
    doc.save(fileName);

    // Return estimated file size
    return doc.output('arraybuffer').byteLength;
  }

  /**
   * Generates CSV file with PapaParse from historical time-series data.
   *
   * CSV Structure:
   * - Metadata header: Location, INSEE, date range, generation time
   * - Column headers: Date, Time, AQI, Pollutants (NO2, O3, PM10, PM2.5, SO2),
   *   Weather (Temperature, Humidity, Wind Speed, Wind Direction, Weather Code)
   * - Data rows: One row per DataPoint
   * - Footer: Data completeness statistics
   *
   * @param data - HistoricalDataResponse from backend (time-series)
   * @param fileName - Output CSV file name
   * @returns number - File size in bytes
   */
  private generateCSV(data: HistoricalDataResponse, fileName: string): number {
    const csvData: any[] = [];

    // Metadata header
    csvData.push({
      'Info': 'Location',
      'Value': data.commune.name
    });
    csvData.push({
      'Info': 'INSEE Code',
      'Value': data.commune.inseeCode
    });
    csvData.push({
      'Info': 'Date Range',
      'Value': `${data.dateRange.startDate} to ${data.dateRange.endDate}`
    });
    csvData.push({
      'Info': 'Days Count',
      'Value': data.dateRange.daysCount || 'N/A'
    });
    csvData.push({
      'Info': 'Generated',
      'Value': new Date().toLocaleString()
    });
    csvData.push({});

    // Data completeness
    csvData.push({
      'Info': 'Data Completeness',
      'Value': `${data.dataCompleteness.completenessPercent}%`
    });
    csvData.push({
      'Info': 'Expected Points',
      'Value': data.dataCompleteness.expectedPoints
    });
    csvData.push({
      'Info': 'Actual Points',
      'Value': data.dataCompleteness.actualPoints
    });
    csvData.push({});

    // Column headers for data points
    csvData.push({
      'Date': 'Date',
      'Time': 'Time',
      'AQI': 'AQI',
      'NO2 (μg/m³)': 'NO2',
      'O3 (μg/m³)': 'O3',
      'PM10 (μg/m³)': 'PM10',
      'PM2.5 (μg/m³)': 'PM2.5',
      'SO2 (μg/m³)': 'SO2',
      'Temperature (°C)': 'Temperature',
      'Humidity (%)': 'Humidity',
      'Wind Speed (km/h)': 'Wind Speed',
      'Wind Direction (°)': 'Wind Direction',
      'Weather Code': 'Weather Code'
    });

    // Data points
    if (data.dataPoints && data.dataPoints.length > 0) {
      data.dataPoints.forEach(point => {
        csvData.push({
          'Date': point.date,
          'Time': point.time,
          'AQI': point.aqi || 'N/A',
          'NO2 (μg/m³)': point.no2 || 'N/A',
          'O3 (μg/m³)': point.o3 || 'N/A',
          'PM10 (μg/m³)': point.pm10 || 'N/A',
          'PM2.5 (μg/m³)': point.pm25 || 'N/A',
          'SO2 (μg/m³)': point.so2 || 'N/A',
          'Temperature (°C)': point.temperature || 'N/A',
          'Humidity (%)': point.humidity || 'N/A',
          'Wind Speed (km/h)': point.windSpeed || 'N/A',
          'Wind Direction (°)': point.windDirection || 'N/A',
          'Weather Code': point.weatherCode || 'N/A'
        });
      });
    } else {
      csvData.push({ 'Date': 'No historical data available for this period' });
    }

    // Generate CSV string
    const csv = Papa.unparse(csvData);

    // Create Blob and download
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', fileName);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    return blob.size;
  }

  /**
   * Retrieves export history from localStorage.
   *
   * @returns Observable<ExportRecord[]> - Array of export records, sorted by date (newest first)
   */
  getExportHistory(): Observable<ExportRecord[]> {
    try {
      const historyJson = localStorage.getItem(this.STORAGE_KEY);
      if (!historyJson) {
        return of([]);
      }

      const history: ExportRecord[] = JSON.parse(historyJson);
      history.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      return of(history);
    } catch (error) {
      console.error('Error reading export history from localStorage:', error);
      return of([]);
    }
  }

  /**
   * Deletes a specific export record from history.
   *
   * @param exportId - Unique export ID to delete
   * @returns Observable<void>
   */
  deleteExportRecord(exportId: string): Observable<void> {
    return from(
      new Promise<void>((resolve, reject) => {
        try {
          const historyJson = localStorage.getItem(this.STORAGE_KEY);
          if (!historyJson) {
            resolve();
            return;
          }

          let history: ExportRecord[] = JSON.parse(historyJson);
          history = history.filter(record => record.id !== exportId);
          localStorage.setItem(this.STORAGE_KEY, JSON.stringify(history));
          resolve();
        } catch (error) {
          console.error('Error deleting export record:', error);
          reject(error);
        }
      })
    );
  }

  /**
   * Clears all export history from localStorage.
   *
   * @returns Observable<void>
   */
  clearExportHistory(): Observable<void> {
    return from(
      new Promise<void>((resolve, reject) => {
        try {
          localStorage.removeItem(this.STORAGE_KEY);
          resolve();
        } catch (error) {
          console.error('Error clearing export history:', error);
          reject(error);
        }
      })
    );
  }

  /**
   * Saves export record to localStorage history.
   * Maintains max 50 records (removes oldest when limit exceeded).
   *
   * @param record - Export record to save
   */
  private saveExportToHistory(record: ExportRecord): void {
    try {
      const historyJson = localStorage.getItem(this.STORAGE_KEY);
      let history: ExportRecord[] = historyJson ? JSON.parse(historyJson) : [];

      history.unshift(record);

      if (history.length > this.MAX_HISTORY_RECORDS) {
        history = history.slice(0, this.MAX_HISTORY_RECORDS);
      }

      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(history));
    } catch (error) {
      console.error('Error saving export to history:', error);
    }
  }

  /**
   * Maps ATMO index to RGB color for PDF generation.
   *
   * ATMO Index Color Scale:
   * - 1: Green (Good)
   * - 2: Yellow (Moderate)
   * - 3: Orange (Degraded)
   * - 4: Red (Poor)
   * - 5: Purple (Very Poor)
   * - 6: Maroon (Extremely Poor)
   *
   * @param atmoIndex - ATMO air quality index (1-6)
   * @returns RGB color object { r, g, b }
   */
  private getAQIColor(atmoIndex: number): { r: number; g: number; b: number } {
    switch (atmoIndex) {
      case 1: return { r: 0, g: 228, b: 0 };
      case 2: return { r: 255, g: 255, b: 0 };
      case 3: return { r: 255, g: 126, b: 0 };
      case 4: return { r: 255, g: 0, b: 0 };
      case 5: return { r: 153, g: 0, b: 76 };
      case 6: return { r: 126, g: 0, b: 35 };
      default: return { r: 128, g: 128, b: 128 };
    }
  }
}
