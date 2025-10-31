export interface AirQualityData {
  inseeCode: string;
  commune: string;
  aqi: number;
  aqiLabel: string;
  aqiColor: string;
  pm25?: number;
  pm10?: number;
  o3?: number;
  no2?: number;
  timestamp: Date;
}

/**
 * Detailed air quality measurements with individual pollutants
 */
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

export interface AirQualityMapData {
  latitude: number;
  longitude: number;
  globalIndex: number;
  globalQuality: AirQualityLevel;
  commune: string;
  color: string;
  popupContent: string;
}
