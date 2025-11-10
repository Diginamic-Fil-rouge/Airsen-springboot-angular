/**
 * Map Filter Models
 * Used for filtering communes and air quality data on the map
 */

export interface MapFilter {
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
 * Default map filter
 */
export const DEFAULT_MAP_FILTER: MapFilter = {
  mapStyle: MapStyle.STREETS,
  showHeatmap: false,
  aqiLevels: [],
  departments: [],
  regions: [],
  searchQuery: "",
};
