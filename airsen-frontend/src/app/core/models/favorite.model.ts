// Favorite Models - User's saved locations and indicators

export interface UserFavorite {
  id: number;
  userId: number;
  communeId: number;
  commune: {
    id: number;
    name: string;
    postalCode: string;
    department: string;
    region: string;
  };
  displayName: string;
  isDefault: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateFavoriteRequest {
  communeId: number;
  displayName?: string;
  isDefault?: boolean;
}

export interface UpdateFavoriteRequest {
  displayName?: string;
  isDefault?: boolean;
}

export interface FavoriteWithData extends UserFavorite {
  currentAirQuality?: {
    globalIndex: number;
    globalQuality: string;
    lastUpdate: Date;
  };
  currentWeather?: {
    temperature: number;
    weatherDescription: string;
    icon: string;
    lastUpdate: Date;
  };
}