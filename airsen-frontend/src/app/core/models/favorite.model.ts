import { Commune } from './commune.model';

/**
 * User favorite commune model
 * Represents a saved/bookmarked commune for quick access to environmental data
 */
export interface UserFavorite {
  id: number;
  user: {
    id: number;
  };
  commune: Commune;
  addedAt: Date;
}

export interface Favorite{
    communeInseeCode: string,
    communeName: string,
    departmentName: string,
    regionName: string,
    addedAt: Date
}

export interface FavoriteCheckResponse {
    isFavorited: boolean
}

/**
 * Request model for creating a favorite
 */
export interface CreateFavoriteRequest {
  inseeCode: string;
}

/**
 * Response model for favorite operations with enriched data
 */
export interface FavoriteWithData extends UserFavorite {
  currentAirQuality?: {
    aqi: number;
    aqiLabel: string;
    aqiColor: string;
    timestamp: Date;
  };
  currentWeather?: {
    temperature: number;
    weatherDescription: string;
    timestamp: Date;
  };
}
