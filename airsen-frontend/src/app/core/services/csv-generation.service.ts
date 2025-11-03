import { Injectable } from '@angular/core';
import * as Papa from 'papaparse';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HistoricalDataResponse, DataPoint } from '@/shared/models/export.model';

/**
 * CSV Generation Service
 *
 * Generates CSV files from historical time-series environmental data
 * using PapaParse library. Creates Excel-compatible CSV with:
 * - French column headers
 * - All air quality indicators
 * - All weather indicators
 * - Properly formatted date/time
 * - Auto-download with timestamp filename
 */
@Injectable({
  providedIn: 'root'
})
export class CsvGenerationService {
  constructor(private snackBar: MatSnackBar) {}

  /**
   * Export historical data to CSV file
   *
   * @param data - Historical data response from backend
   * @param filename - Name for the downloaded CSV file
   *
   * @example
   * this.csvGenerationService.exportHistoricalData(data, 'airsen_historic_paris_2025-09-01_2025-10-09.csv');
   */
  exportHistoricalData(data: HistoricalDataResponse, filename: string): void {
    try {
      // Transform data points to CSV-friendly format
      const csvData = data.dataPoints.map(point => this.transformDataPoint(point));

      // Generate CSV string with PapaParse
      const csv = Papa.unparse(csvData, {
        quotes: false,
        delimiter: ',',
        header: true,
        newline: '\n'
      });

      // Download CSV
      this.downloadCSV(csv, filename);
    } catch (error) {
      console.error('CSV generation failed:', error);
      this.snackBar.open('Erreur lors de la génération du CSV', 'Fermer', { duration: 3000 });
      throw error;
    }
  }

  /**
   * Transform a single data point to CSV row format
   */
  private transformDataPoint(point: DataPoint): Record<string, any> {
    return {
      'Date': point.date,
      'Heure': point.time,
      'AQI': point.aqi !== undefined ? point.aqi : '',
      'PM2.5 (µg/m³)': point.pm25 !== undefined ? point.pm25.toFixed(2) : '',
      'PM10 (µg/m³)': point.pm10 !== undefined ? point.pm10.toFixed(2) : '',
      'NO2 (µg/m³)': point.no2 !== undefined ? point.no2.toFixed(2) : '',
      'O3 (µg/m³)': point.o3 !== undefined ? point.o3.toFixed(2) : '',
      'SO2 (µg/m³)': point.so2 !== undefined ? point.so2.toFixed(2) : '',
      'Température (°C)': point.temperature !== undefined ? point.temperature.toFixed(2) : '',
      'Humidité (%)': point.humidity !== undefined ? point.humidity : '',
      'Vitesse du Vent (km/h)': point.windSpeed !== undefined ? point.windSpeed.toFixed(2) : '',
      'Direction du Vent (°)': point.windDirection !== undefined ? point.windDirection : '',
      'Code Météo': point.weatherCode !== undefined ? point.weatherCode : ''
    };
  }

  /**
   * Download CSV file by creating blob and triggering browser download
   */
  private downloadCSV(csv: string, filename: string): void {
    // Create blob from CSV string
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });

    // Create object URL
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    // Set download attributes
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';

    // Trigger download
    document.body.appendChild(link);
    link.click();

    // Cleanup
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}
