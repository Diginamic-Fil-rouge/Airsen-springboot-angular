import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommuneDetailPanelComponent } from './commune-detail-panel.component';
import { CommuneWithAirQuality } from '@/shared/models/commune.model';
import { Weather } from '@/shared/models/weather.model';
import { AirQuality, AirQualityLevel } from '@/shared/models/air-quality.model';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

describe('CommuneDetailPanelComponent', () => {
  let component: CommuneDetailPanelComponent;
  let fixture: ComponentFixture<CommuneDetailPanelComponent>;

  const mockCommune: CommuneWithAirQuality = {
    id: 1,
    inseeCode: '75056',
    name: 'Paris',
    population: 2165423,
    latitude: 48.8566,
    longitude: 2.3522,
    departmentCode: '75',
    regionCode: '11',
    department: {
      id: 1,
      code: '75',
      name: 'Paris',
      regionId: 1,
      region: {
        id: 1,
        code: '11',
        name: 'Île-de-France'
      }
    },
    currentAirQuality: {
      atmoIndex: 45,
      qualifier: 'Bon',
      color: '#4CAF50'
    },
    pollutants: {
      pm25: 12,
      pm10: 20,
      o3: 35,
      no2: 18
    }
  };

  const mockWeather: Weather = {
    id: 1,
    communeId: 1,
    latitude: 48.8566,
    longitude: 2.3522,
    measurementDate: new Date('2025-01-10T14:00:00'),
    temperature: 15.5,
    humidity: 65,
    windSpeed: 12.5,
    windDirection: 180,
    weatherCode: 0,
    createdAt: new Date(),
    updatedAt: new Date()
  };

  const mockAirQuality: AirQuality = {
    id: 1,
    communeId: 1,
    latitude: 48.8566,
    longitude: 2.3522,
    measurementDate: new Date(),
    globalIndex: 45,
    globalQuality: AirQualityLevel.GOOD,
    no2: 18,
    no2Quality: AirQualityLevel.GOOD,
    o3: 35,
    o3Quality: AirQualityLevel.GOOD,
    pm10: 20,
    pm10Quality: AirQualityLevel.GOOD,
    pm25: 12,
    pm25Quality: AirQualityLevel.GOOD,
    so2: 5,
    so2Quality: AirQualityLevel.GOOD,
    createdAt: new Date(),
    updatedAt: new Date()
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CommuneDetailPanelComponent],
      imports: [MatIconModule, MatButtonModule]
    }).compileComponents();

    fixture = TestBed.createComponent(CommuneDetailPanelComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize with null commune', () => {
      expect(component.commune).toBeNull();
    });

    it('should initialize with null weatherData', () => {
      expect(component.weatherData).toBeNull();
    });

    it('should initialize with null airQualityData', () => {
      expect(component.airQualityData).toBeNull();
    });

    it('should initialize with isLoading as false', () => {
      expect(component.isLoading).toBe(false);
    });
  });

  describe('Event Handlers', () => {
    it('should emit close event on onClose', () => {
      spyOn(component.close, 'emit');

      component.onClose();

      expect(component.close.emit).toHaveBeenCalled();
    });

    it('should emit addToFavorites event on onAddToFavorites', () => {
      spyOn(component.addToFavorites, 'emit');

      component.onAddToFavorites();

      expect(component.addToFavorites.emit).toHaveBeenCalled();
    });

    it('should emit exportPDF event on onExportPDF', () => {
      spyOn(component.exportPDF, 'emit');

      component.onExportPDF();

      expect(component.exportPDF.emit).toHaveBeenCalled();
    });
  });

  describe('AQI Value Getter', () => {
    it('should return atmoIndex from commune if available', () => {
      component.commune = mockCommune;

      expect(component.aqiValue).toBe(45);
    });

    it('should return globalIndex from airQualityData if commune has no data', () => {
      component.commune = { ...mockCommune, currentAirQuality: undefined };
      component.airQualityData = mockAirQuality;

      expect(component.aqiValue).toBe(45);
    });

    it('should return null if no data available', () => {
      component.commune = null;
      component.airQualityData = null;

      expect(component.aqiValue).toBeNull();
    });

    it('should prioritize commune data over airQualityData', () => {
      component.commune = mockCommune;
      component.airQualityData = { ...mockAirQuality, globalIndex: 100 };

      expect(component.aqiValue).toBe(45);
    });
  });

  describe('AQI Label Getter', () => {
    it('should return qualifier from commune if available', () => {
      component.commune = mockCommune;

      expect(component.aqiLabel).toBe('Bon');
    });

    it('should return globalQuality from airQualityData if commune has no data', () => {
      component.commune = { ...mockCommune, currentAirQuality: undefined };
      component.airQualityData = mockAirQuality;

      expect(component.aqiLabel).toBe(AirQualityLevel.GOOD);
    });

    it('should return "Inconnu" if no data available', () => {
      component.commune = null;
      component.airQualityData = null;

      expect(component.aqiLabel).toBe('Inconnu');
    });
  });

  describe('AQI Color Getter', () => {
    it('should return color from commune if available', () => {
      component.commune = mockCommune;

      expect(component.aqiColor).toBe('#4CAF50');
    });

    it('should return default color if no data available', () => {
      component.commune = null;

      expect(component.aqiColor).toBe('#999999');
    });
  });

  describe('AQI Background Class Getter', () => {
    it('should return aqi-bg-good for index 0-50', () => {
      component.commune = { ...mockCommune, currentAirQuality: { atmoIndex: 30, qualifier: 'Bon', color: '#4CAF50' } };
      expect(component.aqiBgClass).toBe('aqi-bg-good');
    });

    it('should return aqi-bg-moderate for index 51-100', () => {
      component.commune = { ...mockCommune, currentAirQuality: { atmoIndex: 75, qualifier: 'Moyen', color: '#FFC107' } };
      expect(component.aqiBgClass).toBe('aqi-bg-moderate');
    });

    it('should return aqi-bg-sensitive for index 101-150', () => {
      component.commune = { ...mockCommune, currentAirQuality: { atmoIndex: 125, qualifier: 'Dégradé', color: '#FF9800' } };
      expect(component.aqiBgClass).toBe('aqi-bg-sensitive');
    });

    it('should return aqi-bg-unhealthy for index 151-200', () => {
      component.commune = { ...mockCommune, currentAirQuality: { atmoIndex: 175, qualifier: 'Mauvais', color: '#F44336' } };
      expect(component.aqiBgClass).toBe('aqi-bg-unhealthy');
    });

    it('should return aqi-bg-very for index 201-300', () => {
      component.commune = { ...mockCommune, currentAirQuality: { atmoIndex: 250, qualifier: 'Très mauvais', color: '#9C27B0' } };
      expect(component.aqiBgClass).toBe('aqi-bg-very');
    });

    it('should return aqi-bg-hazardous for index >300', () => {
      component.commune = { ...mockCommune, currentAirQuality: { atmoIndex: 350, qualifier: 'Extrêmement mauvais', color: '#8D2635' } };
      expect(component.aqiBgClass).toBe('aqi-bg-hazardous');
    });

    it('should return aqi-bg-unknown for null index', () => {
      component.commune = null;
      expect(component.aqiBgClass).toBe('aqi-bg-unknown');
    });
  });

  describe('Data Availability Getters', () => {
    it('hasCommuneData should return true when commune is set', () => {
      component.commune = mockCommune;
      expect(component.hasCommuneData).toBe(true);
    });

    it('hasCommuneData should return false when commune is null', () => {
      component.commune = null;
      expect(component.hasCommuneData).toBe(false);
    });

    it('hasWeatherData should return true when weatherData is set', () => {
      component.weatherData = mockWeather;
      expect(component.hasWeatherData).toBe(true);
    });

    it('hasWeatherData should return false when weatherData is null', () => {
      component.weatherData = null;
      expect(component.hasWeatherData).toBe(false);
    });

    it('hasAirQualityData should return true when aqiValue is not null', () => {
      component.commune = mockCommune;
      expect(component.hasAirQualityData).toBe(true);
    });

    it('hasAirQualityData should return false when aqiValue is null', () => {
      component.commune = null;
      component.airQualityData = null;
      expect(component.hasAirQualityData).toBe(false);
    });
  });

  describe('Formatted Population Getter', () => {
    it('should format population with French locale', () => {
      component.commune = mockCommune;
      expect(component.formattedPopulation).toBe('2 165 423');
    });

    it('should return "—" for null population', () => {
      component.commune = { ...mockCommune, population: undefined };
      expect(component.formattedPopulation).toBe('—');
    });

    it('should return "—" for null commune', () => {
      component.commune = null;
      expect(component.formattedPopulation).toBe('—');
    });
  });

  describe('Formatted Density Getter', () => {
    it('should return placeholder for missing area data', () => {
      component.commune = mockCommune;
      expect(component.formattedDensity).toBe('—');
    });
  });

  describe('Commune Information Getters', () => {
    it('communeName should return commune name', () => {
      component.commune = mockCommune;
      expect(component.communeName).toBe('Paris');
    });

    it('communeName should return "—" for null commune', () => {
      component.commune = null;
      expect(component.communeName).toBe('—');
    });

    it('departmentCode should return department code', () => {
      component.commune = mockCommune;
      expect(component.departmentCode).toBe('75');
    });

    it('departmentCode should return "—" for null commune', () => {
      component.commune = null;
      expect(component.departmentCode).toBe('—');
    });

    it('departmentName should return department name', () => {
      component.commune = mockCommune;
      expect(component.departmentName).toBe('Paris');
    });

    it('departmentName should fallback to department code if name not available', () => {
      component.commune = { ...mockCommune, department: undefined };
      expect(component.departmentName).toBe('75');
    });

    it('regionName should return region name', () => {
      component.commune = mockCommune;
      expect(component.regionName).toBe('Île-de-France');
    });

    it('regionName should return "—" if region not available', () => {
      component.commune = { ...mockCommune, department: undefined };
      expect(component.regionName).toBe('—');
    });
  });

  describe('Weather Data Getters', () => {
    it('temperature should return weather temperature', () => {
      component.weatherData = mockWeather;
      expect(component.temperature).toBe(15.5);
    });

    it('temperature should return null if no weather data', () => {
      component.weatherData = null;
      expect(component.temperature).toBeNull();
    });

    it('humidity should return weather humidity', () => {
      component.weatherData = mockWeather;
      expect(component.humidity).toBe(65);
    });

    it('humidity should return null if no weather data', () => {
      component.weatherData = null;
      expect(component.humidity).toBeNull();
    });

    it('windSpeed should return weather wind speed', () => {
      component.weatherData = mockWeather;
      expect(component.windSpeed).toBe(12.5);
    });

    it('windSpeed should return null if no weather data', () => {
      component.weatherData = null;
      expect(component.windSpeed).toBeNull();
    });

    it('weatherCode should return weather code', () => {
      component.weatherData = mockWeather;
      expect(component.weatherCode).toBe(0);
    });

    it('weatherCode should return null if no weather data', () => {
      component.weatherData = null;
      expect(component.weatherCode).toBeNull();
    });
  });

  describe('Data Source Indicators', () => {
    it('airQualityDataSource should return "Non disponible" if no data', () => {
      component.commune = null;
      component.airQualityData = null;
      expect(component.airQualityDataSource).toBe('Non disponible');
    });

    it('airQualityDataSource should return "Mesure directe" if data available', () => {
      component.commune = mockCommune;
      expect(component.airQualityDataSource).toBe('Mesure directe');
    });

    it('weatherDataSource should return "Non disponible" if no data', () => {
      component.weatherData = null;
      expect(component.weatherDataSource).toBe('Non disponible');
    });

    it('weatherDataSource should return "Mesure directe" if data available', () => {
      component.weatherData = mockWeather;
      expect(component.weatherDataSource).toBe('Mesure directe');
    });
  });

  describe('UI State Getters', () => {
    it('showLoadingSkeleton should return true when loading and no data', () => {
      component.isLoading = true;
      component.commune = null;
      expect(component.showLoadingSkeleton).toBe(true);
    });

    it('showLoadingSkeleton should return false when not loading', () => {
      component.isLoading = false;
      component.commune = null;
      expect(component.showLoadingSkeleton).toBe(false);
    });

    it('showLoadingSkeleton should return false when data is available', () => {
      component.isLoading = true;
      component.commune = mockCommune;
      expect(component.showLoadingSkeleton).toBe(false);
    });

    it('showContent should return true when not loading and has data', () => {
      component.isLoading = false;
      component.commune = mockCommune;
      expect(component.showContent).toBe(true);
    });

    it('showContent should return false when loading', () => {
      component.isLoading = true;
      component.commune = mockCommune;
      expect(component.showContent).toBe(false);
    });

    it('showContent should return false when no data', () => {
      component.isLoading = false;
      component.commune = null;
      expect(component.showContent).toBe(false);
    });

    it('showErrorState should return true when not loading and no data', () => {
      component.isLoading = false;
      component.commune = null;
      expect(component.showErrorState).toBe(true);
    });

    it('showErrorState should return false when loading', () => {
      component.isLoading = true;
      component.commune = null;
      expect(component.showErrorState).toBe(false);
    });

    it('showErrorState should return false when data is available', () => {
      component.isLoading = false;
      component.commune = mockCommune;
      expect(component.showErrorState).toBe(false);
    });
  });

  describe('Input Changes', () => {
    it('should handle commune input change', () => {
      component.commune = mockCommune;
      fixture.detectChanges();

      expect(component.communeName).toBe('Paris');
    });

    it('should handle weatherData input change', () => {
      component.weatherData = mockWeather;
      fixture.detectChanges();

      expect(component.hasWeatherData).toBe(true);
    });

    it('should handle airQualityData input change', () => {
      component.airQualityData = mockAirQuality;
      fixture.detectChanges();

      expect(component.hasAirQualityData).toBe(true);
    });

    it('should handle isLoading input change', () => {
      component.isLoading = true;
      fixture.detectChanges();

      expect(component.showLoadingSkeleton).toBe(true);
    });
  });
});
