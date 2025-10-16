// Weather Models based on Open-Meteo API and backend WeatherData entity

export interface WeatherData {
  id: number;
  communeId: number;
  latitude: number;
  longitude: number;
  measurementDate: Date;
  temperature: number;
  humidity: number;
  pressure: number;
  windSpeed: number;
  windDirection: number;
  precipitation: number;
  visibility: number;
  uvIndex: number;
  weatherCode: number;
  weatherDescription: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface WeatherForecast {
  commune: string;
  communeId: number;
  currentWeather: WeatherData;
  hourlyForecast: WeatherHourly[];
  dailyForecast: WeatherDaily[];
  lastUpdated: Date;
}

export interface WeatherHourly {
  time: Date;
  temperature: number;
  humidity: number;
  precipitation: number;
  windSpeed: number;
  weatherCode: number;
  weatherDescription: string;
  icon: string;
}

export interface WeatherDaily {
  date: Date;
  temperatureMin: number;
  temperatureMax: number;
  humidity: number;
  precipitation: number;
  precipitationProbability: number;
  windSpeed: number;
  windDirection: number;
  uvIndex: number;
  weatherCode: number;
  weatherDescription: string;
  icon: string;
  sunrise: Date;
  sunset: Date;
}

export interface WeatherHistory {
  commune: string;
  communeId: number;
  startDate: Date;
  endDate: Date;
  measurements: WeatherData[];
  averages: {
    temperature: number;
    humidity: number;
    pressure: number;
    windSpeed: number;
    precipitation: number;
  };
  extremes: {
    maxTemperature: { value: number; date: Date };
    minTemperature: { value: number; date: Date };
    maxWindSpeed: { value: number; date: Date };
    maxPrecipitation: { value: number; date: Date };
  };
}

export interface WeatherAlert {
  id: number;
  alertType: WeatherAlertType;
  severity: WeatherSeverity;
  location: string;
  scope: AlertScope;
  title: string;
  description: string;
  startTime: Date;
  endTime: Date;
  isActive: boolean;
  color: string;
  icon: string;
}

export enum WeatherAlertType {
  STORM = 'STORM',
  RAIN = 'RAIN',
  SNOW = 'SNOW',
  WIND = 'WIND',
  TEMPERATURE = 'TEMPERATURE',
  FOG = 'FOG'
}

export enum WeatherSeverity {
  LOW = 'LOW',
  MODERATE = 'MODERATE',
  HIGH = 'HIGH',
  EXTREME = 'EXTREME'
}

// Weather condition mappings for icons and colors
export interface WeatherCondition {
  code: number;
  description: string;
  icon: string;
  color: string;
  category: WeatherCategory;
}

export enum WeatherCategory {
  CLEAR = 'CLEAR',
  CLOUDY = 'CLOUDY',
  RAINY = 'RAINY',
  SNOWY = 'SNOWY',
  STORMY = 'STORMY',
  FOGGY = 'FOGGY'
}

// For chart visualization
export interface WeatherChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    borderColor: string;
    backgroundColor: string;
    yAxisID: string;
    type: 'line' | 'bar';
    tension: number;
  }[];
}