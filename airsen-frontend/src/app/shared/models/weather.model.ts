/**
 * Core weather data model
 * Simplified representation of current weather measurements for a commune
 */
export interface WeatherData {
  inseeCode: string;
  temperature: number;
  feelsLike: number;
  humidity: number;
  windSpeed: number;
  weatherCode: number;
  weatherDescription: string;
  timestamp: Date;
}

/**
 * Detailed weather measurements with additional fields
 */
export interface Weather {
  id: number;
  communeId: number;
  latitude: number;
  longitude: number;
  measurementDate: Date;
  temperature: number;
  humidity: number;
  windSpeed: number;
  windDirection: number;
  weatherCode: number;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Weather forecast data including hourly and daily forecasts
 */
export interface WeatherForecast {
  commune: string;
  communeId: number;
  currentWeather: Weather;
  hourlyForecast: WeatherHourly[];
  dailyForecast: WeatherDaily[];
  lastUpdated: Date;
}

/**
 * Hourly weather forecast
 */
export interface WeatherHourly {
  time: Date;
  temperature: number;
  humidity: number;
  windSpeed: number;
  weatherCode: number;
  weatherDescription: string;
  icon: string;
}

/**
 * Daily weather forecast
 */
export interface WeatherDaily {
  date: Date;
  temperatureMin: number;
  temperatureMax: number;
  humidity: number;
  windSpeed: number;
  windDirection: number;
  weatherCode: number;
  weatherDescription: string;
  icon: string;
}

/**
 * Historical weather data for date range analysis
 */
export interface WeatherHistory {
  commune: string;
  communeId: number;
  startDate: Date;
  endDate: Date;
  measurements: Weather[];
  averages: {
    temperature: number;
    humidity: number;
    windSpeed: number;
  };
  extremes: {
    maxTemperature: { value: number; date: Date };
    minTemperature: { value: number; date: Date };
    maxWindSpeed: { value: number; date: Date };
    maxPrecipitation: { value: number; date: Date };
  };
}

/**
 * Chart data for weather visualization
 */
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
