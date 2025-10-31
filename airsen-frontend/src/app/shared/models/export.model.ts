export interface ExportRecord {
  id: string;
  userId: number;
  exportType: ExportType;
  format: ExportFormat;
  locationName: string;
  inseeCode?: string;
  fileSize: number;
  createdAt: Date;
}

export enum ExportType {
  AIR_QUALITY = "AIR_QUALITY",
  WEATHER = "WEATHER",
  COMBINED = "COMBINED",
}

export enum ExportFormat {
  PDF = "PDF",
  CSV = "CSV",
}

export interface ExportDataRequest {
  inseeCode: string;
  locationName: string;
  startDate?: Date;
  endDate?: Date;
  exportType: ExportType;
  format: ExportFormat;
}

export interface ExportData {
  commune: string;
  inseeCode: string;
  generatedAt: Date;
  airQuality?: {
    measurements: Array<{
      date: Date;
      aqi: number;
      aqiLabel: string;
      pm25?: number;
      pm10?: number;
      o3?: number;
      no2?: number;
    }>;
  };
  weather?: {
    measurements: Array<{
      date: Date;
      temperature: number;
      feelsLike: number;
      humidity: number;
      windSpeed: number;
      weatherDescription: string;
    }>;
  };
}

export interface ExportHistoryItem {
  id: string;
  fileName: string;
  format: ExportFormat;
  locationName: string;
  createdAt: Date;
  fileSize: number;
}
