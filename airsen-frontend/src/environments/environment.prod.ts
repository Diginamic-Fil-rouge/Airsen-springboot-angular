// Production Environment Configuration
export const environment = {
  production: true,
  apiUrl: 'https://api.airsen.fr/api/v1', // To be configured with actual production URL
  mapbox: {
    accessToken: 'YOUR_PRODUCTION_MAPBOX_TOKEN_HERE' // To be configured
  },
  features: {
    enableDebugMode: false,
    enableMockData: false,
    enableAnalytics: true
  },
  cache: {
    defaultTtl: 600000, // 10 minutes
    airQualityTtl: 3600000, // 1 hour
    weatherTtl: 1800000 // 30 minutes
  },
  pagination: {
    defaultPageSize: 20,
    maxPageSize: 100
  },
  notifications: {
    duration: 5000, // 5 seconds
    position: 'top-right'
  }
};
