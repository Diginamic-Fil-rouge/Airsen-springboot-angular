import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, map, of } from 'rxjs';
import { environment } from 'src/environments/environment';
import jsPDF from 'jspdf';
import * as Papa from 'papaparse';
import {
  ExportRecord,
  ExportDataRequest,
  ExportData,
  ExportFormat,
  ExportHistoryItem
} from '@/shared/models';

/**
 * ExportService - Client-Side PDF/CSV Generation
 *
 * Handles data export functionality for AIRSEN application:
 * - Fetches aggregated data from backend API
 * - Generates PDF documents with jsPDF (air quality + weather data)
 * - Generates CSV files with PapaParse
 * - Manages export history in localStorage (max 50 records)
 */
@Injectable({
  providedIn: "root",
})
export class ExportService {
  private readonly apiUrl = `${environment.apiUrl}/communes/{inseeCode}/export-data`;
  private readonly STORAGE_KEY = "airsen_export_history";
  private readonly MAX_HISTORY_RECORDS = 50;
  private http = inject(HttpClient);

  /**
   * Fetches export data from backend and generates file in specified format.
   *
   * Flow:
   * 1. POST request to /api/v1/communes/{inseeCode}/export-data with inseeCode, startDate, endDate
   * 2. Backend aggregates air quality and weather data
   * 3. Generate PDF or CSV based on format parameter
   * 4. Save export record to localStorage history
   * 5. Auto-download generated file
   *
   * @param request - Export request with inseeCode, date range, format
   * @returns Observable<ExportRecord> - Export metadata with file info
   */
  exportData(request: ExportDataRequest): Observable<ExportRecord> {
    return this.http
      .post<ExportData>(`${this.apiUrl}/data`, {
        inseeCode: request.inseeCode,
        startDate: request.startDate,
        endDate: request.endDate,
      })
      .pipe(
        map((data: ExportData) => {
          // Generate unique export ID: exp_{timestamp}_{random}
          const exportId = `exp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

          // Generate file based on format
          let fileSize = 0;
          const startDateStr = request.startDate?.toISOString().split("T")[0] || "latest";
          const endDateStr = request.endDate?.toISOString().split("T")[0] || "latest";
          const fileName = `airsen_export_${
            data.commune
          }_${startDateStr}_${endDateStr}.${request.format.toLowerCase()}`;

          if (request.format === ExportFormat.PDF) {
            fileSize = this.generatePDF(data, {
              locationName: data.commune,
              startDate: startDateStr,
              endDate: endDateStr,
              fileName: fileName,
            });
          } else if (request.format === ExportFormat.CSV) {
            fileSize = this.generateCSV(data, {
              locationName: data.commune,
              startDate: startDateStr,
              endDate: endDateStr,
              fileName: fileName,
            });
          }

          // Create export record
          const exportRecord: ExportRecord = {
            id: exportId,
            userId: 0, // Will be set by component
            locationName: data.commune,
            inseeCode: data.inseeCode,
            exportType: request.exportType,
            format: request.format,
            fileSize: fileSize,
            createdAt: new Date(),
          };

          // Save to localStorage history
          this.saveExportToHistory(exportRecord);

          return exportRecord;
        })
      );
  }

  /**
   * Generates PDF document with jsPDF.
   *
   * PDF Structure:
   * - Header: "AIRSEN - Environmental Data Export"
   * - Location: Commune name, INSEE code
   * - Date Range: Start date to end date
   * - Air Quality Section:
   *   - ATMO Index with color coding (1=green, 6=red)
   *   - Pollutants: NO2, O3, PM10, PM2.5, SO2
   * - Weather Section:
   *   - Temperature (°C)
   *   - Humidity (%)
   *   - Precipitation (mm)
   *   - Wind Speed (km/h)
   * - Footer: Generated date
   *
   * @param data - Aggregated export data from backend
   * @param metadata - Export metadata (location, dates, fileName)
   * @returns number - Estimated file size in bytes
   */
  generatePDF(
    data: ExportData,
    metadata: { locationName: string; startDate: string; endDate: string; fileName: string }
  ): number {
    const doc = new jsPDF();
    let yPosition = 20;

    // Header
    doc.setFontSize(18);
    doc.setFont("helvetica", "bold");
    doc.text("AIRSEN - Environmental Data Export", 105, yPosition, { align: "center" });
    yPosition += 15;

    // Location and Date Range
    doc.setFontSize(12);
    doc.setFont("helvetica", "normal");
    doc.text(`Location: ${metadata.locationName} (INSEE: ${data.inseeCode})`, 20, yPosition);
    yPosition += 8;
    doc.text(`Period: ${metadata.startDate} to ${metadata.endDate}`, 20, yPosition);
    yPosition += 15;

    // Air Quality Section
    doc.setFontSize(14);
    doc.setFont("helvetica", "bold");
    doc.text("Air Quality Data", 20, yPosition);
    yPosition += 10;

    doc.setFontSize(11);
    doc.setFont("helvetica", "normal");

    if (data.airQuality && data.airQuality.measurements && data.airQuality.measurements.length > 0) {
      data.airQuality.measurements.forEach((aqi) => {
        // ATMO Index with color coding
        const indexColor = this.getAQIColor(aqi.aqi);
        doc.setTextColor(indexColor.r, indexColor.g, indexColor.b);
        doc.text(`Date: ${aqi.date} - AQI: ${aqi.aqi} (${aqi.aqiLabel})`, 25, yPosition);
        doc.setTextColor(0, 0, 0); // Reset to black
        yPosition += 6;

        // Pollutants
        doc.text(
          `  NO2: ${aqi.no2 || "N/A"} μg/m³, O3: ${aqi.o3 || "N/A"} μg/m³, PM10: ${aqi.pm10 || "N/A"} μg/m³`,
          25,
          yPosition
        );
        yPosition += 6;
        doc.text(`  PM2.5: ${aqi.pm25 || "N/A"} μg/m³`, 25, yPosition);
        yPosition += 8;

        // Page break if needed
        if (yPosition > 270) {
          doc.addPage();
          yPosition = 20;
        }
      });
    } else {
      doc.text("No air quality data available for this period.", 25, yPosition);
      yPosition += 10;
    }

    yPosition += 10;

    // Weather Section
    doc.setFontSize(14);
    doc.setFont("helvetica", "bold");
    doc.text("Weather Data", 20, yPosition);
    yPosition += 10;

    doc.setFontSize(11);
    doc.setFont("helvetica", "normal");

    if (data.weather && data.weather.measurements && data.weather.measurements.length > 0) {
      data.weather.measurements.forEach((weather) => {
        doc.text(`Date: ${weather.date}`, 25, yPosition);
        yPosition += 6;
        doc.text(
          `  Temperature: ${weather.temperature}°C, Feels Like: ${weather.feelsLike}°C, Humidity: ${weather.humidity}%`,
          25,
          yPosition
        );
        yPosition += 6;
        doc.text(`  Wind Speed: ${weather.windSpeed} km/h, ${weather.weatherDescription}`, 25, yPosition);
        yPosition += 8;

        // Page break if needed
        if (yPosition > 270) {
          doc.addPage();
          yPosition = 20;
        }
      });
    } else {
      doc.text("No weather data available for this period.", 25, yPosition);
      yPosition += 10;
    }

    // Footer
    doc.setFontSize(9);
    doc.setTextColor(128, 128, 128);
    const totalPages = doc.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      doc.text(`Generated: ${new Date().toLocaleString()} - Page ${i} of ${totalPages}`, 105, 285, { align: "center" });
    }

    // Save and download
    doc.save(metadata.fileName);

    // Estimate file size (approximate)
    return doc.output("arraybuffer").byteLength;
  }

  /**
   * Generates CSV file with PapaParse.
   *
   * CSV Structure:
   * - Header Row: Date, Type, ATMO Index, Qualifier, NO2, O3, PM10, PM2.5, SO2, Temperature, Humidity, Precipitation, Wind Speed
   * - Data Rows: Combined air quality and weather data by date
   *
   * @param data - Aggregated export data from backend
   * @param metadata - Export metadata (location, dates, fileName)
   * @returns number - Estimated file size in bytes
   */
  generateCSV(
    data: ExportData,
    metadata: { locationName: string; startDate: string; endDate: string; fileName: string }
  ): number {
    const csvData: any[] = [];

    // Header row
    csvData.push({
      Date: "",
      Location: metadata.locationName,
      "INSEE Code": data.inseeCode,
      Period: `${metadata.startDate} to ${metadata.endDate}`,
      Generated: new Date().toLocaleString(),
    });

    // Empty row
    csvData.push({});

    // Air Quality Data
    csvData.push({ Date: "Air Quality Data" });
    csvData.push({
      Date: "Date",
      "ATMO Index": "ATMO Index",
      Qualifier: "Qualifier",
      "NO2 (μg/m³)": "NO2",
      "O3 (μg/m³)": "O3",
      "PM10 (μg/m³)": "PM10",
      "PM2.5 (μg/m³)": "PM2.5",
      "SO2 (μg/m³)": "SO2",
    });

    if (data.airQuality && data.airQuality.measurements && data.airQuality.measurements.length > 0) {
      data.airQuality.measurements.forEach((aqi) => {
        csvData.push({
          Date: aqi.date,
          AQI: aqi.aqi,
          Label: aqi.aqiLabel,
          "NO2 (μg/m³)": aqi.no2 || "N/A",
          "O3 (μg/m³)": aqi.o3 || "N/A",
          "PM10 (μg/m³)": aqi.pm10 || "N/A",
          "PM2.5 (μg/m³)": aqi.pm25 || "N/A",
        });
      });
    } else {
      csvData.push({ Date: "No air quality data available" });
    }

    // Empty row
    csvData.push({});

    // Weather Data
    csvData.push({ Date: "Weather Data" });
    csvData.push({
      Date: "Date",
      "Temperature (°C)": "Temperature",
      "Humidity (%)": "Humidity",
      "Precipitation (mm)": "Precipitation",
      "Wind Speed (km/h)": "Wind Speed",
    });

    if (data.weather && data.weather.measurements && data.weather.measurements.length > 0) {
      data.weather.measurements.forEach((weather) => {
        csvData.push({
          Date: weather.date,
          "Temperature (°C)": weather.temperature,
          "Feels Like (°C)": weather.feelsLike,
          "Humidity (%)": weather.humidity,
          "Wind Speed (km/h)": weather.windSpeed,
          Description: weather.weatherDescription,
        });
      });
    } else {
      csvData.push({ Date: "No weather data available" });
    }

    // Generate CSV string
    const csv = Papa.unparse(csvData);

    // Create Blob and download
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", metadata.fileName);
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    // Return file size
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
      // Sort by date descending (newest first)
      history.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      return of(history);
    } catch (error) {
      console.error("Error reading export history from localStorage:", error);
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
          history = history.filter((record) => record.id !== exportId);
          localStorage.setItem(this.STORAGE_KEY, JSON.stringify(history));
          resolve();
        } catch (error) {
          console.error("Error deleting export record:", error);
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
          console.error("Error clearing export history:", error);
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

      // Add new record
      history.unshift(record);

      // Maintain max 50 records (remove oldest)
      if (history.length > this.MAX_HISTORY_RECORDS) {
        history = history.slice(0, this.MAX_HISTORY_RECORDS);
      }

      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(history));
    } catch (error) {
      console.error("Error saving export to history:", error);
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
      case 1:
        return { r: 0, g: 228, b: 0 }; // Green
      case 2:
        return { r: 255, g: 255, b: 0 }; // Yellow
      case 3:
        return { r: 255, g: 126, b: 0 }; // Orange
      case 4:
        return { r: 255, g: 0, b: 0 }; // Red
      case 5:
        return { r: 153, g: 0, b: 76 }; // Purple
      case 6:
        return { r: 126, g: 0, b: 35 }; // Maroon
      default:
        return { r: 128, g: 128, b: 128 }; // Gray (unknown)
    }
  }
}
