
export interface Commune {
  id: number;
  inseeCode: string;
  name: string;
  population?: number;
  latitude?: number;
  longitude?: number;
  departmentCode?: string;
  regionCode?: string;
  department?: Department;
}

export interface Department {
  id: number;
  code: string;
  name: string;
  regionId?: number;
  region?: Region;
}

export interface Region {
  id: number;
  code: string;
  name: string;
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

/**
 * Extended commune interface with current air quality data
 * Used for map display and visualization where air quality indicators
 * need to be shown alongside geographic information
 */
export interface CommuneWithAirQuality extends Commune {
  currentAirQuality?: {
    atmoIndex: number;
    qualifier: string;
    color: string;
  };
}
