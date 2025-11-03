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
 * Commune information
 */
export interface CommuneInfo {
  inseeCode: string;
  name: string;
  departmentName: string;
  regionName: string;
  population: number;
  latitude: number;
  longitude: number;
}

/**
 * Air quality data snapshot
 */
export interface AirQualityDataSnapshot {
  aqi: number;
  pm25: number;
  pm10: number;
  no2: number;
  o3: number;
  so2: number;
  measurementDate: string;
}

/**
 * Weather data snapshot
 */
export interface WeatherDataSnapshot {
  temperature: number;
  humidity: number;
  windSpeed: number;
  windDirection: number;
  weatherCode: number;
  measurementDate: string;
}

/**
 * Data quality metadata
 */
export interface DataQuality {
  airQualityFreshness: DataFreshness;
  weatherFreshness: DataFreshness;
  dataSource: DataSource;
  cacheAge: string;
  cacheFreshness: number;
}

/**
 * Date range specification
 */
export interface DateRange {
  startDate: string;
  endDate: string;
  daysCount?: number;
}

/**
 * Single data point in time series
 */
export interface DataPoint {
  date: string;
  time: string;
  aqi?: number;
  pm25?: number;
  pm10?: number;
  no2?: number;
  o3?: number;
  so2?: number;
  temperature?: number;
  humidity?: number;
  windSpeed?: number;
  windDirection?: number;
  weatherCode?: number;
}

/**
 * Data completeness statistics
 */
export interface DataCompleteness {
  expectedPoints: number;
  actualPoints: number;
  completenessPercent: number;
}

/**
 * Export Data Response - Current snapshot data for PDF generation
 */
export interface ExportDataResponse {
  commune: CommuneInfo;
  airQuality: AirQualityDataSnapshot;
  weather: WeatherDataSnapshot;
  dataQuality: DataQuality;
}

/**
 * Historical Data Response - Time-series data for CSV generation
 */
export interface HistoricalDataResponse {
  commune: Pick<CommuneInfo, 'inseeCode' | 'name'>;
  dateRange: DateRange;
  dataPoints: DataPoint[];
  dataCompleteness: DataCompleteness;
}
