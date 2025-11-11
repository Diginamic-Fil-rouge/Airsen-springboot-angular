import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import jsPDF from 'jspdf';
import { ExportDataResponse } from '@/shared/models/export.model';

/**
 * PDF Generation Service
 *
 * Generates formatted PDF reports from current environmental data
 * using jsPDF library. Creates professional reports with:
 * - AIRSEN branding
 * - Commune information (name, INSEE, department, region, population)
 * - Current air quality data with AQI color badge
 * - Current weather data
 * - Data quality metadata
 * - Auto-download with timestamp filename
 */
@Injectable({
  providedIn: 'root'
})
export class PdfGenerationService {
  private readonly pageWidth = 210; // A4 width in mm
  private readonly pageHeight = 297; // A4 height in mm
  private readonly margin = 20;
  private readonly contentWidth = this.pageWidth - 2 * this.margin;

  constructor(private snackBar: MatSnackBar) {}

  /**
   * Generate and download PDF report for current commune data
   *
   * @param data - Export data response from backend
   * @param communeName - Name of the commune for report title
   *
   * @example
   * this.pdfGenerationService.generateCurrentReport(data, 'Paris 16e Arrondissement');
   */
  generateCurrentReport(data: ExportDataResponse, communeName: string): void {
    try {
      const doc = new jsPDF('p', 'mm', 'a4');
      let yPosition = this.margin;

      // Header
      yPosition = this.addHeader(doc, communeName, yPosition);

      // Section 1: Commune Information
      yPosition = this.addCommuneSection(doc, data, yPosition);

      // Section 2: Air Quality
      yPosition = this.addAirQualitySection(doc, data, yPosition);

      // Section 3: Weather
      yPosition = this.addWeatherSection(doc, data, yPosition);

      // Section 4: Data Quality (optional - only if provided by backend)
      if (data.exportMetadata) {
        yPosition = this.addDataQualitySection(doc, data, yPosition);
      }

      // Footer
      this.addFooter(doc);

      // Download
      const filename = this.generateFilename(communeName);
      doc.save(filename);
    } catch (error) {
      console.error('PDF generation failed:', error);
      this.snackBar.open('Erreur lors de la génération du PDF', 'Fermer', { duration: 3000 });
      throw error;
    }
  }

  /**
   * Add header section with branding and title
   */
  private addHeader(doc: jsPDF, communeName: string, yPosition: number): number {
    // Title
    doc.setFontSize(24);
    doc.setFont('helvetica', 'bold');
    doc.text('AIRSEN', this.margin, yPosition);
    yPosition += 10;

    // Subtitle
    doc.setFontSize(16);
    doc.text('Rapport Qualité de l\'Air', this.margin, yPosition);
    yPosition += 8;

    // Commune name
    doc.setFontSize(12);
    doc.text(communeName, this.margin, yPosition);
    yPosition += 6;

    // Generation date
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    const now = new Date();
    doc.text(`Généré le ${this.formatDateFull(now)}`, this.margin, yPosition);
    yPosition += 8;

    // Horizontal line
    doc.setDrawColor(100);
    doc.line(this.margin, yPosition, this.pageWidth - this.margin, yPosition);
    yPosition += 10;

    return yPosition;
  }

  /**
   * Add commune information section
   */
  private addCommuneSection(doc: jsPDF, data: ExportDataResponse, yPosition: number): number {
    if (!data.commune) {
      return yPosition;
    }

    // Title
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Informations Commune', this.margin, yPosition);
    yPosition += 7;

    // Data rows
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');

    const rows = [
      { label: 'Nom', value: data.commune.name || 'N/A' },
      { label: 'Code INSEE', value: data.commune.inseeCode || 'N/A' },
      { label: 'Département', value: data.commune.department?.name || 'N/A' },
      { label: 'Région', value: data.commune.department?.region?.name || 'N/A' },
      { label: 'Population', value: data.commune.population ? `${this.formatNumber(data.commune.population)} habitants` : 'N/A' },
      { label: 'Coordonnées', value: (data.commune.latitude && data.commune.longitude) ? `${data.commune.latitude.toFixed(4)}, ${data.commune.longitude.toFixed(4)}` : 'N/A' }
    ];

    yPosition = this.addTable(doc, rows, yPosition);
    yPosition += 8;

    return yPosition;
  }

  /**
   * Add air quality section with AQI color badge
   */
  private addAirQualitySection(doc: jsPDF, data: ExportDataResponse, yPosition: number): number {
    if (!data.airQuality) {
      return yPosition;
    }

    // Title
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Qualité de l\'Air', this.margin, yPosition);
    yPosition += 7;

    // Measurement date
    if (data.airQuality.measurementDate) {
      doc.setFontSize(9);
      doc.setFont('helvetica', 'normal');
      doc.text(`Date de mesure: ${this.formatDateFull(new Date(data.airQuality.measurementDate))}`, this.margin, yPosition);
      yPosition += 6;
    }

    // AQI with color badge
    const aqi = data.airQuality.atmIndex ?? 0;
    const aqiColor = data.airQuality.atmoColor ?? this.getAQIColor(aqi);
    const aqiLabel = data.airQuality.atmoQual ?? this.getAQILabel(aqi);

    // Draw AQI badge
    doc.setFillColor(...this.hexToRgb(aqiColor));
    doc.rect(this.margin, yPosition, 30, 15, 'F');

    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(255, 255, 255);
    doc.text(`AQI: ${aqi}`, this.margin + 3, yPosition + 10);
    doc.setTextColor(0, 0, 0);

    yPosition += 20;

    // Data table
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');

    const rows = [
      { label: 'Indice AQI', value: `${aqi} (${aqiLabel})` },
      { label: 'PM2.5', value: data.airQuality.pm25 !== undefined ? `${data.airQuality.pm25} µg/m³` : 'N/A' },
      { label: 'PM10', value: data.airQuality.pm10 !== undefined ? `${data.airQuality.pm10} µg/m³` : 'N/A' },
      { label: 'NO2', value: data.airQuality.no2 !== undefined ? `${data.airQuality.no2} µg/m³` : 'N/A' },
      { label: 'O3', value: data.airQuality.o3 !== undefined ? `${data.airQuality.o3} µg/m³` : 'N/A' },
      { label: 'SO2', value: data.airQuality.so2 !== undefined ? `${data.airQuality.so2} µg/m³` : 'N/A' }
    ];

    yPosition = this.addTable(doc, rows, yPosition);
    yPosition += 8;

    return yPosition;
  }

  /**
   * Add weather section
   */
  private addWeatherSection(doc: jsPDF, data: ExportDataResponse, yPosition: number): number {
    if (!data.weather) {
      return yPosition;
    }

    // Check if we have room, add page break if needed
    if (yPosition > this.pageHeight - 60) {
      doc.addPage();
      yPosition = this.margin;
    }

    // Title
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Conditions Météorologiques', this.margin, yPosition);
    yPosition += 7;

    // Measurement date
    if (data.weather.measurementDate) {
      doc.setFontSize(9);
      doc.setFont('helvetica', 'normal');
      doc.text(`Date de mesure: ${this.formatDateFull(new Date(data.weather.measurementDate))}`, this.margin, yPosition);
      yPosition += 6;
    }

    // Data table
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');

    const rows = [
      { label: 'Température', value: data.weather.temperature !== undefined ? `${data.weather.temperature}°C` : 'N/A' },
      { label: 'Humidité', value: data.weather.humidity !== undefined ? `${data.weather.humidity}%` : 'N/A' },
      { label: 'Vitesse du vent', value: data.weather.windSpeed !== undefined ? `${data.weather.windSpeed} km/h` : 'N/A' },
      { label: 'Direction du vent', value: data.weather.windDirection !== undefined ? `${data.weather.windDirection}°` : 'N/A' }
    ];

    yPosition = this.addTable(doc, rows, yPosition);
    yPosition += 8;

    return yPosition;
  }

  /**
   * Add data quality section
   */
  private addDataQualitySection(doc: jsPDF, data: ExportDataResponse, yPosition: number): number {
    // Check if we have room, add page break if needed
    if (yPosition > this.pageHeight - 50) {
      doc.addPage();
      yPosition = this.margin;
    }

    // Title
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Qualité des Données', this.margin, yPosition);
    yPosition += 7;

    // Data table
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');

    const rows = [
      { label: 'Fraîcheur AQI', value: data.exportMetadata?.dataFreshness?.airQuality || 'N/A' },
      { label: 'Fraîcheur Météo', value: data.exportMetadata?.dataFreshness?.weather || 'N/A' },
      { label: 'Généré le', value: data.exportMetadata?.generatedAt ? this.formatDateFull(new Date(data.exportMetadata.generatedAt)) : 'N/A' }
    ];

    yPosition = this.addTable(doc, rows, yPosition);

    return yPosition;
  }

  /**
   * Add footer with generation info
   */
  private addFooter(doc: jsPDF): void {
    const footerY = this.pageHeight - 10;

    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(128, 128, 128);

    doc.text('Généré par AIRSEN - Plateforme d\'Information Environnementale', this.margin, footerY);
    doc.text(`Page 1 sur 1`, this.pageWidth - this.margin - 20, footerY);
  }

  /**
   * Add table with label-value rows
   */
  private addTable(
    doc: jsPDF,
    rows: Array<{ label: string; value: string }>,
    startY: number
  ): number {
    const labelWidth = 60;
    const valueWidth = this.contentWidth - labelWidth - 5;
    const rowHeight = 8;
    let yPosition = startY;

    rows.forEach((row, index) => {
      const isEvenRow = index % 2 === 0;

      // Background color for alternating rows
      if (isEvenRow) {
        doc.setFillColor(249, 249, 249);
        doc.rect(this.margin, yPosition - 3, this.contentWidth, rowHeight, 'F');
      }

      // Border
      doc.setDrawColor(200);
      doc.rect(this.margin, yPosition - 3, this.contentWidth, rowHeight);

      // Label
      doc.setFont('helvetica', 'bold');
      doc.text(row.label, this.margin + 2, yPosition + 2);

      // Value
      doc.setFont('helvetica', 'normal');
      const textWidth = doc.getTextWidth(row.value);
      if (textWidth > valueWidth) {
        const splitText = doc.splitTextToSize(row.value, valueWidth);
        doc.text(splitText, this.margin + labelWidth + 2, yPosition + 2);
      } else {
        doc.text(row.value, this.margin + labelWidth + 2, yPosition + 2);
      }

      yPosition += rowHeight;
    });

    return yPosition;
  }

  /**
   * Get AQI color based on index value
   */
  private getAQIColor(aqi: number): string {
    if (aqi <= 50) return '#4CAF50'; // Green - Bon
    if (aqi <= 100) return '#FFC107'; // Yellow - Moyen
    if (aqi <= 150) return '#FF9800'; // Orange - Dégradé
    if (aqi <= 200) return '#F44336'; // Red - Mauvais
    return '#9C27B0'; // Purple - Très Mauvais
  }

  /**
   * Get AQI label in French
   */
  private getAQILabel(aqi: number): string {
    if (aqi <= 50) return 'Bon';
    if (aqi <= 100) return 'Moyen';
    if (aqi <= 150) return 'Dégradé';
    if (aqi <= 200) return 'Mauvais';
    return 'Très Mauvais';
  }

  /**
   * Convert hex color to RGB
   */
  private hexToRgb(hex: string): [number, number, number] {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result
      ? [parseInt(result[1], 16), parseInt(result[2], 16), parseInt(result[3], 16)]
      : [0, 0, 0];
  }

  /**
   * Format date to French format: DD/MM/YYYY HH:MM
   */
  private formatDateFull(date: Date): string {
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${day}/${month}/${year} à ${hours}:${minutes}`;
  }

  /**
   * Format date to YYYY-MM-DD format for filename
   */
  private formatDateFilename(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  /**
   * Generate filename for PDF
   */
  private generateFilename(communeName: string): string {
    const sanitizedName = communeName
      .toLowerCase()
      .replace(/\s+/g, '_')
      .replace(/[^a-z0-9_]/g, '');
    const date = this.formatDateFilename(new Date());

    return `airsen_report_${sanitizedName}_${date}.pdf`;
  }

  /**
   * Format number with thousands separator
   */
  private formatNumber(num: number): string {
    return num.toLocaleString('fr-FR');
  }
}
