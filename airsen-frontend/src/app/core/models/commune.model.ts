// Commune and Geographic Models based on INSEE data

export interface Commune {
  id: number;
  inseeCode: string;
  name: string;
  postalCode: string;
  latitude: number;
  longitude: number;
  population: number;
  area: number;
  density: number;
  departmentId: number;
  department?: Department;
  currentAirQuality?: AirQuality;
  currentWeather?: WeatherData;
}

export interface Department {
  id: number;
  code: string;
  name: string;
  regionId: number;
  region?: Region;
  communes?: Commune[];
  communeCount: number;
  population: number;
  area: number;
}

export interface Region {
  id: number;
  code: string;
  name: string;
  departments?: Department[];
  departmentCount: number;
  communeCount: number;
  population: number;
  area: number;
}

export interface CommuneSearchRequest {
  query: string;
  departmentCode?: string;
  regionCode?: string;
  limit?: number;
}

export interface CommuneSearchResult {
  communes: Commune[];
  totalCount: number;
  hasMore: boolean;
}

export interface CommuneDetails extends Commune {
  demographics: {
    population: number;
    density: number;
    area: number;
  };
  airQualityStats: {
    averageIndex: number;
    bestQuality: string;
    worstQuality: string;
    lastMonth: {
      goodDays: number;
      moderateDays: number;
      poorDays: number;
      veryPoorDays: number;
    };
  };
  weatherStats: {
    averageTemperature: number;
    averageHumidity: number;
    averagePrecipitation: number;
    lastMonth: {
      sunnyDays: number;
      cloudyDays: number;
      rainyDays: number;
    };
  };
}

// Import from air-quality and weather models to avoid circular dependency
interface AirQuality {
  id: number;
  communeId: number;
  globalIndex: number;
  globalQuality: string;
  measurementDate: Date;
}

interface WeatherData {
  id: number;
  communeId: number;
  temperature: number;
  weatherDescription: string;
  measurementDate: Date;
}