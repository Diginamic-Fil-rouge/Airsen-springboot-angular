/**
 * Map Filter Models
 * Used for filtering communes and air quality data on the map
 */

export interface MapFilter {
  // Pollutant filter
  pollutantType: PollutantType;

  // Time range filter
  timeRange: TimeRange;

  // Map display options
  mapStyle: MapStyle;
  showHeatmap: boolean;

  // Commune filters
  aqiLevels: string[];
  departments: string[];
  regions: string[];

  // Search/location
  searchQuery: string;
  boundingBox?: BoundingBox;
}

export enum PollutantType {
  ALL = "ALL",
  PM25 = "PM25",
  PM10 = "PM10",
  O3 = "O3",
  NO2 = "NO2",
  SO2 = "SO2",
}

export enum TimeRange {
  NOW = "NOW",
  HOUR_1 = "HOUR_1",
  HOURS_24 = "HOURS_24",
  DAYS_7 = "DAYS_7",
  DAYS_30 = "DAYS_30",
}

export enum MapStyle {
  STREETS = "STREETS",
  SATELLITE = "SATELLITE",
  TERRAIN = "TERRAIN",
  DARK = "DARK",
}

export interface BoundingBox {
  north: number;
  south: number;
  east: number;
  west: number;
}

/**
 * Get label for pollutant type
 */
export function getPollutantLabel(type: PollutantType): string {
  const labels: Record<PollutantType, string> = {
    [PollutantType.ALL]: "Tous les polluants",
    [PollutantType.PM25]: "PM2.5",
    [PollutantType.PM10]: "PM10",
    [PollutantType.O3]: "Ozone (O₃)",
    [PollutantType.NO2]: "Dioxyde d'azote (NO₂)",
    [PollutantType.SO2]: "Dioxyde de soufre (SO₂)",
  };
  return labels[type] || type;
}

/**
 * Get label for time range
 */
export function getTimeRangeLabel(range: TimeRange): string {
  const labels: Record<TimeRange, string> = {
    [TimeRange.NOW]: "Maintenant",
    [TimeRange.HOUR_1]: "Dernière heure",
    [TimeRange.HOURS_24]: "Dernières 24 heures",
    [TimeRange.DAYS_7]: "7 derniers jours",
    [TimeRange.DAYS_30]: "30 derniers jours",
  };
  return labels[range] || range;
}

/**
 * Default map filter
 */
export const DEFAULT_MAP_FILTER: MapFilter = {
  pollutantType: PollutantType.ALL,
  timeRange: TimeRange.NOW,
  mapStyle: MapStyle.STREETS,
  showHeatmap: false,
  aqiLevels: [],
  departments: [],
  regions: [],
  searchQuery: "",
};
