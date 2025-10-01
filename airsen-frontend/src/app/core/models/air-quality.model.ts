// Air Quality Models based on ATMO France API and backend AirQuality entity

export interface AirQuality {
  id: number;
  communeId: number;
  latitude: number;
  longitude: number;
  measurementDate: Date;
  globalIndex: number;
  globalQuality: AirQualityLevel;
  no2: number;
  no2Quality: AirQualityLevel;
  o3: number;
  o3Quality: AirQualityLevel;
  pm10: number;
  pm10Quality: AirQualityLevel;
  pm25: number;
  pm25Quality: AirQualityLevel;
  so2: number;
  so2Quality: AirQualityLevel;
  createdAt: Date;
  updatedAt: Date;
}

export enum AirQualityLevel {
  GOOD = 'GOOD',
  MODERATE = 'MODERATE', 
  POOR = 'POOR',
  VERY_POOR = 'VERY_POOR',
  UNKNOWN = 'UNKNOWN'
}

export interface AirQualityIndicator {
  pollutant: string;
  value: number;
  unit: string;
  quality: AirQualityLevel;
  color: string;
  icon: string;
  description: string;
}

export interface AirQualityHistory {
  commune: string;
  communeId: number;
  startDate: Date;
  endDate: Date;
  measurements: AirQuality[];
  averages: {
    globalIndex: number;
    no2: number;
    o3: number;
    pm10: number;
    pm25: number;
    so2: number;
  };
}

export interface AirQualityAlert {
  id: number;
  alertLevel: AlertLevel;
  pollutant: string;
  threshold: number;
  currentValue: number;
  location: string;
  scope: AlertScope;
  message: string;
  isActive: boolean;
  startTime: Date;
  endTime?: Date;
  createdAt: Date;
}

export enum AlertLevel {
  INFO = 'INFO',
  WARNING = 'WARNING',
  ALERT = 'ALERT',
  EMERGENCY = 'EMERGENCY'
}

export enum AlertScope {
  COMMUNE = 'COMMUNE',
  DEPARTMENT = 'DEPARTMENT', 
  REGION = 'REGION',
  NATIONAL = 'NATIONAL'
}

export interface AirQualityExportRequest {
  communeId: number;
  startDate: Date;
  endDate: Date;
  format: ExportFormat;
  indicators: string[];
}

export enum ExportFormat {
  PDF = 'PDF',
  CSV = 'CSV',
  EXCEL = 'EXCEL'
}

// For chart visualization
export interface AirQualityChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    borderColor: string;
    backgroundColor: string;
    tension: number;
  }[];
}

// For map visualization  
export interface AirQualityMapData {
  latitude: number;
  longitude: number;
  globalIndex: number;
  globalQuality: AirQualityLevel;
  commune: string;
  color: string;
  popupContent: string;
}