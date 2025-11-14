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
  GOOD = "GOOD",
  MODERATE = "MODERATE",
  POOR = "POOR",
  VERY_POOR = "VERY_POOR",
  UNKNOWN = "UNKNOWN",
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

/**
 * Get health recommendation based on ATMO France index (1-6)
 * ATMO index is the French standard for air quality measurement
 */
export function getHealthRecommendation(atmoIndex: number): string {
  const recommendations: Record<number, string> = {
    1: "La qualité de l'air est satisfaisante. Profitez de vos activités en plein air.",
    2: "Qualité de l'air acceptable pour la plupart. Les personnes sensibles devraient limiter les efforts prolongés en extérieur.",
    3: "Les personnes sensibles peuvent ressentir des effets. Limitez les activités extérieures prolongées.",
    4: "Tout le monde peut commencer à ressentir des effets. Les personnes sensibles peuvent ressentir des effets plus graves. Évitez les activités extérieures prolongées.",
    5: "Alerte sanitaire : tout le monde peut ressentir des effets plus graves sur la santé. Évitez toutes les activités extérieures.",
    6: "Alerte d'urgence sanitaire. Toute la population est susceptible d'être affectée. Restez à l'intérieur et gardez les fenêtres fermées.",
  };
  return recommendations[atmoIndex] || "Données non disponibles.";
}
