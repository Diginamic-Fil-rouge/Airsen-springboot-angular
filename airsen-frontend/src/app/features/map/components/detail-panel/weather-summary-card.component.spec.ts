import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WeatherSummaryCardComponent } from './weather-summary-card.component';
import { Weather } from '@/shared/models/weather.model';
import { MatIconModule } from '@angular/material/icon';

describe('WeatherSummaryCardComponent', () => {
  let component: WeatherSummaryCardComponent;
  let fixture: ComponentFixture<WeatherSummaryCardComponent>;

  const createMockWeather = (overrides?: Partial<Weather>): Weather => ({
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
    updatedAt: new Date(),
    ...overrides
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [WeatherSummaryCardComponent],
      imports: [MatIconModule]
    }).compileComponents();

    fixture = TestBed.createComponent(WeatherSummaryCardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize with null weather', () => {
      expect(component.weather).toBeNull();
    });
  });

  describe('Data Availability', () => {
    it('hasData should return true when weather is set', () => {
      component.weather = createMockWeather();
      expect(component.hasData).toBe(true);
    });

    it('hasData should return false when weather is null', () => {
      component.weather = null;
      expect(component.hasData).toBe(false);
    });

    it('hasData should return false when weather is undefined', () => {
      component.weather = undefined as any;
      expect(component.hasData).toBe(false);
    });
  });

  describe('Temperature Display', () => {
    it('should display temperature rounded with unit', () => {
      component.weather = createMockWeather({ temperature: 15.5 });
      expect(component.displayTemperature).toBe('16°C');
    });

    it('should round down temperature', () => {
      component.weather = createMockWeather({ temperature: 15.3 });
      expect(component.displayTemperature).toBe('15°C');
    });

    it('should handle negative temperature', () => {
      component.weather = createMockWeather({ temperature: -5.2 });
      expect(component.displayTemperature).toBe('-5°C');
    });

    it('should handle zero temperature', () => {
      component.weather = createMockWeather({ temperature: 0 });
      expect(component.displayTemperature).toBe('0°C');
    });

    it('should return "—" for null temperature', () => {
      component.weather = createMockWeather({ temperature: null as any });
      expect(component.displayTemperature).toBe('—');
    });

    it('should return "—" for no weather data', () => {
      component.weather = null;
      expect(component.displayTemperature).toBe('—');
    });
  });

  describe('Wind Speed Display', () => {
    it('hasWindSpeed should return true when windSpeed is set', () => {
      component.weather = createMockWeather({ windSpeed: 12.5 });
      expect(component.hasWindSpeed).toBe(true);
    });

    it('hasWindSpeed should return false when windSpeed is null', () => {
      component.weather = createMockWeather({ windSpeed: null as any });
      expect(component.hasWindSpeed).toBe(false);
    });

    it('should display wind speed rounded with unit', () => {
      component.weather = createMockWeather({ windSpeed: 12.5 });
      expect(component.displayWindSpeed).toBe('13 km/h');
    });

    it('should handle zero wind speed', () => {
      component.weather = createMockWeather({ windSpeed: 0 });
      expect(component.displayWindSpeed).toBe('0 km/h');
    });

    it('should return "—" for null wind speed', () => {
      component.weather = createMockWeather({ windSpeed: null as any });
      expect(component.displayWindSpeed).toBe('—');
    });
  });

  describe('Humidity Display', () => {
    it('hasHumidity should return true when humidity is set', () => {
      component.weather = createMockWeather({ humidity: 65 });
      expect(component.hasHumidity).toBe(true);
    });

    it('hasHumidity should return false when humidity is null', () => {
      component.weather = createMockWeather({ humidity: null as any });
      expect(component.hasHumidity).toBe(false);
    });

    it('should display humidity rounded with percentage', () => {
      component.weather = createMockWeather({ humidity: 65.7 });
      expect(component.displayHumidity).toBe('66%');
    });

    it('should handle 100% humidity', () => {
      component.weather = createMockWeather({ humidity: 100 });
      expect(component.displayHumidity).toBe('100%');
    });

    it('should handle 0% humidity', () => {
      component.weather = createMockWeather({ humidity: 0 });
      expect(component.displayHumidity).toBe('0%');
    });

    it('should return "—" for null humidity', () => {
      component.weather = createMockWeather({ humidity: null as any });
      expect(component.displayHumidity).toBe('—');
    });
  });

  describe('Wind Direction Display', () => {
    it('hasWindDirection should return true when windDirection is set', () => {
      component.weather = createMockWeather({ windDirection: 180 });
      expect(component.hasWindDirection).toBe(true);
    });

    it('hasWindDirection should return false when windDirection is null', () => {
      component.weather = createMockWeather({ windDirection: null as any });
      expect(component.hasWindDirection).toBe(false);
    });

    it('should return "N" for 0 degrees', () => {
      component.weather = createMockWeather({ windDirection: 0 });
      expect(component.displayWindDirection).toBe('N');
    });

    it('should return "NE" for 45 degrees', () => {
      component.weather = createMockWeather({ windDirection: 45 });
      expect(component.displayWindDirection).toBe('NE');
    });

    it('should return "E" for 90 degrees', () => {
      component.weather = createMockWeather({ windDirection: 90 });
      expect(component.displayWindDirection).toBe('E');
    });

    it('should return "SE" for 135 degrees', () => {
      component.weather = createMockWeather({ windDirection: 135 });
      expect(component.displayWindDirection).toBe('SE');
    });

    it('should return "S" for 180 degrees', () => {
      component.weather = createMockWeather({ windDirection: 180 });
      expect(component.displayWindDirection).toBe('S');
    });

    it('should return "SO" for 225 degrees', () => {
      component.weather = createMockWeather({ windDirection: 225 });
      expect(component.displayWindDirection).toBe('SO');
    });

    it('should return "O" for 270 degrees', () => {
      component.weather = createMockWeather({ windDirection: 270 });
      expect(component.displayWindDirection).toBe('O');
    });

    it('should return "NO" for 315 degrees', () => {
      component.weather = createMockWeather({ windDirection: 315 });
      expect(component.displayWindDirection).toBe('NO');
    });

    it('should handle 360 degrees (wraps to N)', () => {
      component.weather = createMockWeather({ windDirection: 360 });
      expect(component.displayWindDirection).toBe('N');
    });

    it('should return "—" for null wind direction', () => {
      component.weather = createMockWeather({ windDirection: null as any });
      expect(component.displayWindDirection).toBe('—');
    });
  });

  describe('Measurement Date', () => {
    beforeEach(() => {
      jasmine.clock().install();
      jasmine.clock().mockDate(new Date('2025-01-10T14:30:00'));
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('hasMeasurementDate should return true when measurementDate is set', () => {
      component.weather = createMockWeather();
      expect(component.hasMeasurementDate).toBe(true);
    });

    it('hasMeasurementDate should return false when measurementDate is null', () => {
      component.weather = createMockWeather({ measurementDate: null as any });
      expect(component.hasMeasurementDate).toBe(false);
    });

    it('should return "À l\'instant" for measurement less than 1 minute ago', () => {
      component.weather = createMockWeather({ measurementDate: new Date('2025-01-10T14:29:50') });
      expect(component.formattedMeasurementDate).toBe("À l'instant");
    });

    it('should return "Il y a 1 minute" for measurement 1 minute ago', () => {
      component.weather = createMockWeather({ measurementDate: new Date('2025-01-10T14:29:00') });
      expect(component.formattedMeasurementDate).toBe('Il y a 1 minute');
    });

    it('should return "Il y a X minutes" for measurements less than 60 minutes ago', () => {
      component.weather = createMockWeather({ measurementDate: new Date('2025-01-10T14:00:00') });
      expect(component.formattedMeasurementDate).toBe('Il y a 30 minutes');
    });

    it('should return formatted date for measurements older than 60 minutes', () => {
      component.weather = createMockWeather({ measurementDate: new Date('2025-01-10T13:00:00') });
      const result = component.formattedMeasurementDate;
      expect(result).toContain('10');
      expect(result).toContain('janv.');
      expect(result).toContain('13:00');
    });

    it('should return empty string for null measurement date', () => {
      component.weather = createMockWeather({ measurementDate: null as any });
      expect(component.formattedMeasurementDate).toBe('');
    });
  });

  describe('Weather Icon Mapping', () => {
    it('should return "wb_sunny" for clear sky (code 0)', () => {
      component.weather = createMockWeather({ weatherCode: 0 });
      expect(component.weatherIcon).toBe('wb_sunny');
    });

    it('should return "wb_cloudy" for partly cloudy (code 1)', () => {
      component.weather = createMockWeather({ weatherCode: 1 });
      expect(component.weatherIcon).toBe('wb_cloudy');
    });

    it('should return "wb_cloudy" for overcast (code 3)', () => {
      component.weather = createMockWeather({ weatherCode: 3 });
      expect(component.weatherIcon).toBe('wb_cloudy');
    });

    it('should return "foggy" for fog (code 45)', () => {
      component.weather = createMockWeather({ weatherCode: 45 });
      expect(component.weatherIcon).toBe('foggy');
    });

    it('should return "foggy" for fog (code 48)', () => {
      component.weather = createMockWeather({ weatherCode: 48 });
      expect(component.weatherIcon).toBe('foggy');
    });

    it('should return "grain" for drizzle (code 51)', () => {
      component.weather = createMockWeather({ weatherCode: 51 });
      expect(component.weatherIcon).toBe('grain');
    });

    it('should return "grain" for drizzle (code 55)', () => {
      component.weather = createMockWeather({ weatherCode: 55 });
      expect(component.weatherIcon).toBe('grain');
    });

    it('should return "water_drop" for rain (code 61)', () => {
      component.weather = createMockWeather({ weatherCode: 61 });
      expect(component.weatherIcon).toBe('water_drop');
    });

    it('should return "water_drop" for rain (code 65)', () => {
      component.weather = createMockWeather({ weatherCode: 65 });
      expect(component.weatherIcon).toBe('water_drop');
    });

    it('should return "ac_unit" for snow (code 71)', () => {
      component.weather = createMockWeather({ weatherCode: 71 });
      expect(component.weatherIcon).toBe('ac_unit');
    });

    it('should return "ac_unit" for snow (code 75)', () => {
      component.weather = createMockWeather({ weatherCode: 75 });
      expect(component.weatherIcon).toBe('ac_unit');
    });

    it('should return "shower" for rain showers (code 80)', () => {
      component.weather = createMockWeather({ weatherCode: 80 });
      expect(component.weatherIcon).toBe('shower');
    });

    it('should return "shower" for rain showers (code 82)', () => {
      component.weather = createMockWeather({ weatherCode: 82 });
      expect(component.weatherIcon).toBe('shower');
    });

    it('should return "thunderstorm" for thunderstorm (code 95)', () => {
      component.weather = createMockWeather({ weatherCode: 95 });
      expect(component.weatherIcon).toBe('thunderstorm');
    });

    it('should return "thunderstorm" for thunderstorm with hail (code 99)', () => {
      component.weather = createMockWeather({ weatherCode: 99 });
      expect(component.weatherIcon).toBe('thunderstorm');
    });

    it('should return "cloud" for unknown weather code', () => {
      component.weather = createMockWeather({ weatherCode: 999 });
      expect(component.weatherIcon).toBe('cloud');
    });

    it('should return "help" for null weather code', () => {
      component.weather = createMockWeather({ weatherCode: null as any });
      expect(component.weatherIcon).toBe('help');
    });
  });

  describe('Weather Description Mapping', () => {
    it('should return "Ciel dégagé" for code 0', () => {
      component.weather = createMockWeather({ weatherCode: 0 });
      expect(component.weatherDescription).toBe('Ciel dégagé');
    });

    it('should return "Principalement dégagé" for code 1', () => {
      component.weather = createMockWeather({ weatherCode: 1 });
      expect(component.weatherDescription).toBe('Principalement dégagé');
    });

    it('should return "Partiellement nuageux" for code 2', () => {
      component.weather = createMockWeather({ weatherCode: 2 });
      expect(component.weatherDescription).toBe('Partiellement nuageux');
    });

    it('should return "Couvert" for code 3', () => {
      component.weather = createMockWeather({ weatherCode: 3 });
      expect(component.weatherDescription).toBe('Couvert');
    });

    it('should return "Brouillard" for code 45', () => {
      component.weather = createMockWeather({ weatherCode: 45 });
      expect(component.weatherDescription).toBe('Brouillard');
    });

    it('should return "Bruine légère" for code 51', () => {
      component.weather = createMockWeather({ weatherCode: 51 });
      expect(component.weatherDescription).toBe('Bruine légère');
    });

    it('should return "Pluie légère" for code 61', () => {
      component.weather = createMockWeather({ weatherCode: 61 });
      expect(component.weatherDescription).toBe('Pluie légère');
    });

    it('should return "Pluie modérée" for code 63', () => {
      component.weather = createMockWeather({ weatherCode: 63 });
      expect(component.weatherDescription).toBe('Pluie modérée');
    });

    it('should return "Neige légère" for code 71', () => {
      component.weather = createMockWeather({ weatherCode: 71 });
      expect(component.weatherDescription).toBe('Neige légère');
    });

    it('should return "Averses légères" for code 80', () => {
      component.weather = createMockWeather({ weatherCode: 80 });
      expect(component.weatherDescription).toBe('Averses légères');
    });

    it('should return "Orage" for code 95', () => {
      component.weather = createMockWeather({ weatherCode: 95 });
      expect(component.weatherDescription).toBe('Orage');
    });

    it('should return code as fallback for unknown code', () => {
      component.weather = createMockWeather({ weatherCode: 999 });
      expect(component.weatherDescription).toBe('Code météo: 999');
    });

    it('should return "Inconnu" for null code', () => {
      component.weather = createMockWeather({ weatherCode: null as any });
      expect(component.weatherDescription).toBe('Inconnu');
    });
  });

  describe('Edge Cases', () => {
    it('should handle all weather fields being null', () => {
      component.weather = createMockWeather({
        temperature: null as any,
        humidity: null as any,
        windSpeed: null as any,
        windDirection: null as any,
        weatherCode: null as any,
        measurementDate: null as any
      });

      expect(component.displayTemperature).toBe('—');
      expect(component.hasHumidity).toBe(false);
      expect(component.hasWindSpeed).toBe(false);
      expect(component.hasWindDirection).toBe(false);
      expect(component.hasMeasurementDate).toBe(false);
      expect(component.weatherIcon).toBe('help');
      expect(component.weatherDescription).toBe('Inconnu');
    });

    it('should handle partial weather data', () => {
      component.weather = createMockWeather({
        temperature: 15,
        humidity: null as any,
        windSpeed: 10,
        windDirection: null as any
      });

      expect(component.displayTemperature).toBe('15°C');
      expect(component.hasHumidity).toBe(false);
      expect(component.displayWindSpeed).toBe('10 km/h');
      expect(component.hasWindDirection).toBe(false);
    });
  });

  describe('Input Changes', () => {
    it('should react to weather input changes', () => {
      component.weather = createMockWeather({ temperature: 15 });
      fixture.detectChanges();
      expect(component.displayTemperature).toBe('15°C');

      component.weather = createMockWeather({ temperature: 25 });
      fixture.detectChanges();
      expect(component.displayTemperature).toBe('25°C');
    });

    it('should react to weather being set to null', () => {
      component.weather = createMockWeather();
      fixture.detectChanges();
      expect(component.hasData).toBe(true);

      component.weather = null;
      fixture.detectChanges();
      expect(component.hasData).toBe(false);
    });
  });
});
