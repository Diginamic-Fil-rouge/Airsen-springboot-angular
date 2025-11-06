/**
 * AQI Station Model for Map Display
 * Represents a monitoring station with air quality data
 */

export interface AqiStation {
  id: number;
  inseeCode: string;
  name: string;
  latitude: number;
  longitude: number;

  // Current AQI Data
  currentAqi: number;
  aqiLevel: AqiLevel;
  aqiColor: string;
  lastUpdated: Date;

  // Pollutant Measurements (µg/m³)
  pollutants: {
    pm25?: PollutantData;
    pm10?: PollutantData;
    o3?: PollutantData;
    no2?: PollutantData;
    so2?: PollutantData;
    co?: PollutantData;
  };

  // Additional Info
  population?: number;
  stationType: StationType;
  isFavorite?: boolean;
}

export interface PollutantData {
  value: number;
  level: AqiLevel;
  unit: string;
  lastUpdated: Date;
}

export enum AqiLevel {
  GOOD = 'GOOD',                          // 0-50: Bon
  MODERATE = 'MODERATE',                  // 51-100: Moyen
  UNHEALTHY_SENSITIVE = 'UNHEALTHY_SENSITIVE',  // 101-150: Dégradé
  UNHEALTHY = 'UNHEALTHY',                // 151-200: Mauvais
  VERY_UNHEALTHY = 'VERY_UNHEALTHY',      // 201-300: Très mauvais
  HAZARDOUS = 'HAZARDOUS',                // 301+: Extrêmement mauvais
  UNKNOWN = 'UNKNOWN'                     // No data
}

export enum StationType {
  GOVERNMENT = 'GOVERNMENT',
  PRIVATE = 'PRIVATE',
  COMMUNITY = 'COMMUNITY'
}

/**
 * Helper function to get AQI color based on level
 */
export function getAqiColor(level: AqiLevel): string {
  const colorMap: Record<AqiLevel, string> = {
    [AqiLevel.GOOD]: '#00E400',
    [AqiLevel.MODERATE]: '#FFFF00',
    [AqiLevel.UNHEALTHY_SENSITIVE]: '#FF7E00',
    [AqiLevel.UNHEALTHY]: '#FF0000',
    [AqiLevel.VERY_UNHEALTHY]: '#8F3F97',
    [AqiLevel.HAZARDOUS]: '#7E0023',
    [AqiLevel.UNKNOWN]: '#999999'
  };
  return colorMap[level] || '#999999';
}

/**
 * Helper function to get AQI label in French
 */
export function getAqiLabel(level: AqiLevel): string {
  const labelMap: Record<AqiLevel, string> = {
    [AqiLevel.GOOD]: 'Bon',
    [AqiLevel.MODERATE]: 'Moyen',
    [AqiLevel.UNHEALTHY_SENSITIVE]: 'Dégradé',
    [AqiLevel.UNHEALTHY]: 'Mauvais',
    [AqiLevel.VERY_UNHEALTHY]: 'Très mauvais',
    [AqiLevel.HAZARDOUS]: 'Extrêmement mauvais',
    [AqiLevel.UNKNOWN]: 'Pas de données'
  };
  return labelMap[level] || 'Inconnu';
}

/**
 * Get AQI level from numeric value
 */
export function getAqiLevelFromValue(aqi: number): AqiLevel {
  if (aqi <= 50) return AqiLevel.GOOD;
  if (aqi <= 100) return AqiLevel.MODERATE;
  if (aqi <= 150) return AqiLevel.UNHEALTHY_SENSITIVE;
  if (aqi <= 200) return AqiLevel.UNHEALTHY;
  if (aqi <= 300) return AqiLevel.VERY_UNHEALTHY;
  if (aqi > 300) return AqiLevel.HAZARDOUS;
  return AqiLevel.UNKNOWN;
}

/**
 * Get health recommendation based on AQI level
 */
export function getHealthRecommendation(level: AqiLevel): string {
  const recommendations: Record<AqiLevel, string> = {
    [AqiLevel.GOOD]: 'La qualité de l\'air est satisfaisante. Profitez de vos activités en plein air.',
    [AqiLevel.MODERATE]: 'Qualité de l\'air acceptable pour la plupart. Les personnes sensibles devraient limiter les efforts prolongés en extérieur.',
    [AqiLevel.UNHEALTHY_SENSITIVE]: 'Les personnes sensibles peuvent ressentir des effets. Limitez les activités extérieures prolongées.',
    [AqiLevel.UNHEALTHY]: 'Tout le monde peut commencer à ressentir des effets. Les personnes sensibles peuvent ressentir des effets plus graves. Évitez les activités extérieures prolongées.',
    [AqiLevel.VERY_UNHEALTHY]: 'Alerte sanitaire : tout le monde peut ressentir des effets plus graves sur la santé. Évitez toutes les activités extérieures.',
    [AqiLevel.HAZARDOUS]: 'Alerte d\'urgence sanitaire. Toute la population est susceptible d\'être affectée. Restez à l\'intérieur et gardez les fenêtres fermées.',
    [AqiLevel.UNKNOWN]: 'Données non disponibles.'
  };
  return recommendations[level] || 'Pas de recommandation disponible.';
}
