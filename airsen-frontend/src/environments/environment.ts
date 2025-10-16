// Development Environment Configuration
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  mapbox: {
    accessToken: 'YOUR_MAPBOX_TOKEN_HERE' // To be configured
  },
  features: {
    enableDebugMode: true,
    enableMockData: false,
    enableAnalytics: false
  },
  cache: {
    defaultTtl: 300000, // 5 minutes
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