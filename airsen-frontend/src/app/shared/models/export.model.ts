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


/**
 * Data freshness status indicator
 */
export enum DataFreshness {
  FRESH = 'FRESH',
  STALE = 'STALE',
  VERY_STALE = 'VERY_STALE'
}

/**
 * Data source indicator
 */
export enum DataSource {
  CACHE = 'CACHE',
  API = 'API',
  FALLBACK = 'FALLBACK'
}

/**
 * Department information (nested in commune)
 */
export interface DepartmentInfo {
  name: string;
  departmentCode: string;
  region: {
    name: string;
    regionCode: string;
  };
}

/**
 * Commune information - Backend Response
 */
export interface CommuneInfo {
  inseeCode: string;
  name: string;
  population: number;
  latitude: number;
  longitude: number;
  department: DepartmentInfo;
}

/**
 * Air quality data snapshot for PDF - Backend Response
 * Uses ATMO France field names (atmIndex, atmoQual, atmoColor)
 */
export interface AirQualityDataSnapshot {
  measurementDate: string;
  atmIndex: number;      // ⚠️ ATMO France index (1-6), NOT "aqi"
  atmoQual: string;      // ⚠️ Quality label (Bon, Moyen, etc.), NOT "aqiLabel"
  atmoColor: string;     // ⚠️ Hex color code, NOT "aqiColor"
  no2?: number;
  o3?: number;
  pm10?: number;
  pm25?: number;
  so2?: number;
  createdAt: string;
}

/**
 * Weather data snapshot for PDF - Backend Response
 */
export interface WeatherDataSnapshot {
  temperature?: number;
  humidity?: number;
  windSpeed?: number;
  windDirection?: number;
  weatherCode?: number;
  measurementDate?: string;
}

/**
 * Export metadata with data freshness info
 */
export interface ExportMetadata {
  generatedAt: string;
  dataFreshness: {
    airQuality: string;
    weather: string;
  };
}

/**
 * Date range specification - Backend Response
 */
export interface DateRange {
  start: string;        // ISO date format: YYYY-MM-DD
  end: string;          // ISO date format: YYYY-MM-DD
}

/**
 * Air quality data in historical data point
 * Uses different field names than PDF snapshot (aqi, qualifier, color)
 */
export interface HistoricalAirQuality {
  aqi: number;          // ⚠️ Same as atmIndex, but backend uses "aqi" here
  qualifier: string;    // ⚠️ Same as atmoQual, but backend uses "qualifier" here
  color: string;        // Hex color code
  no2?: number;
  o3?: number;
  pm10?: number;
  pm25?: number;
  so2?: number;
}

/**
 * Single data point in time series - Backend Response
 */
export interface DataPoint {
  timestamp: string;    // ISO datetime format: YYYY-MM-DDTHH:MM:SS
  airQuality?: HistoricalAirQuality | null;
  weather?: {
    temperature?: number;
    humidity?: number;
    windSpeed?: number;
    windDirection?: number;
    weatherCode?: number;
  } | null;
}

/**
 * Data completeness statistics
 */
export interface DataCompleteness {
  airQuality: number;   // Percentage (0-100)
  weather: number;      // Percentage (0-100)
}

/**
 * Export Data Response - Current snapshot data for PDF generation
 * Endpoint: GET /api/v1/communes/{inseeCode}/export-data
 */
export interface ExportDataResponse {
  commune: CommuneInfo;
  airQuality: AirQualityDataSnapshot | null;
  weather: WeatherDataSnapshot | null;
  exportMetadata: ExportMetadata;
}

/**
 * Historical Data Response - Time-series data for CSV generation
 * Endpoint: GET /api/v1/communes/{inseeCode}/historical-data?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
 */
export interface HistoricalDataResponse {
  commune: {
    name: string;
    inseeCode: string;
  };
  dateRange: DateRange;
  dataPoints: DataPoint[];
  summary: {
    totalDataPoints: number;
    completeness: DataCompleteness;
  };
}
