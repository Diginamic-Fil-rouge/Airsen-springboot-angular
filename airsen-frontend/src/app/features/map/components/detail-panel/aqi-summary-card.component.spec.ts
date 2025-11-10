import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AqiSummaryCardComponent } from './aqi-summary-card.component';
import { AirQuality, AirQualityLevel } from '@/shared/models/air-quality.model';
import { MatIconModule } from '@angular/material/icon';

describe('AqiSummaryCardComponent', () => {
  let component: AqiSummaryCardComponent;
  let fixture: ComponentFixture<AqiSummaryCardComponent>;

  const createMockAirQuality = (globalIndex: number): AirQuality => ({
    id: 1,
    communeId: 1,
    latitude: 48.8566,
    longitude: 2.3522,
    measurementDate: new Date(),
    globalIndex,
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
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AqiSummaryCardComponent],
      imports: [MatIconModule]
    }).compileComponents();

    fixture = TestBed.createComponent(AqiSummaryCardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize with null airQuality', () => {
      expect(component.airQuality).toBeNull();
    });
  });

  describe('AQI Background Class', () => {
    it('should return aqi-bg-good for index 0-50', () => {
      component.airQuality = createMockAirQuality(30);
      expect(component.aqiBgClass).toBe('aqi-bg-good');
    });

    it('should return aqi-bg-good for index exactly 50', () => {
      component.airQuality = createMockAirQuality(50);
      expect(component.aqiBgClass).toBe('aqi-bg-good');
    });

    it('should return aqi-bg-moderate for index 51-100', () => {
      component.airQuality = createMockAirQuality(75);
      expect(component.aqiBgClass).toBe('aqi-bg-moderate');
    });

    it('should return aqi-bg-moderate for index exactly 100', () => {
      component.airQuality = createMockAirQuality(100);
      expect(component.aqiBgClass).toBe('aqi-bg-moderate');
    });

    it('should return aqi-bg-sensitive for index 101-150', () => {
      component.airQuality = createMockAirQuality(125);
      expect(component.aqiBgClass).toBe('aqi-bg-sensitive');
    });

    it('should return aqi-bg-sensitive for index exactly 150', () => {
      component.airQuality = createMockAirQuality(150);
      expect(component.aqiBgClass).toBe('aqi-bg-sensitive');
    });

    it('should return aqi-bg-unhealthy for index 151-200', () => {
      component.airQuality = createMockAirQuality(175);
      expect(component.aqiBgClass).toBe('aqi-bg-unhealthy');
    });

    it('should return aqi-bg-unhealthy for index exactly 200', () => {
      component.airQuality = createMockAirQuality(200);
      expect(component.aqiBgClass).toBe('aqi-bg-unhealthy');
    });

    it('should return aqi-bg-very for index 201-300', () => {
      component.airQuality = createMockAirQuality(250);
      expect(component.aqiBgClass).toBe('aqi-bg-very');
    });

    it('should return aqi-bg-very for index exactly 300', () => {
      component.airQuality = createMockAirQuality(300);
      expect(component.aqiBgClass).toBe('aqi-bg-very');
    });

    it('should return aqi-bg-hazardous for index >300', () => {
      component.airQuality = createMockAirQuality(350);
      expect(component.aqiBgClass).toBe('aqi-bg-hazardous');
    });

    it('should return aqi-bg-hazardous for very high index', () => {
      component.airQuality = createMockAirQuality(500);
      expect(component.aqiBgClass).toBe('aqi-bg-hazardous');
    });

    it('should handle index 0', () => {
      component.airQuality = createMockAirQuality(0);
      expect(component.aqiBgClass).toBe('aqi-bg-good');
    });

    it('should return aqi-bg-good for null globalIndex', () => {
      component.airQuality = { ...createMockAirQuality(0), globalIndex: null as any };
      expect(component.aqiBgClass).toBe('aqi-bg-good');
    });
  });

  describe('Data Availability', () => {
    it('hasData should return true when airQuality is set', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.hasData).toBe(true);
    });

    it('hasData should return false when airQuality is null', () => {
      component.airQuality = null;
      expect(component.hasData).toBe(false);
    });

    it('hasData should return false when airQuality is undefined', () => {
      component.airQuality = undefined as any;
      expect(component.hasData).toBe(false);
    });
  });

  describe('Display AQI Value', () => {
    it('should display globalIndex as string', () => {
      component.airQuality = createMockAirQuality(75);
      expect(component.displayAqiValue).toBe('75');
    });

    it('should return "—" for null airQuality', () => {
      component.airQuality = null;
      expect(component.displayAqiValue).toBe('—');
    });

    it('should handle zero index', () => {
      component.airQuality = createMockAirQuality(0);
      expect(component.displayAqiValue).toBe('0');
    });

    it('should handle high index', () => {
      component.airQuality = createMockAirQuality(350);
      expect(component.displayAqiValue).toBe('350');
    });
  });

  describe('AQI Quality Label', () => {
    it('should return "Bon" for index 0-50', () => {
      component.airQuality = createMockAirQuality(30);
      expect(component.aqiQualityLabel).toBe('Bon');
    });

    it('should return "Moyen" for index 51-100', () => {
      component.airQuality = createMockAirQuality(75);
      expect(component.aqiQualityLabel).toBe('Moyen');
    });

    it('should return "Dégradé" for index 101-150', () => {
      component.airQuality = createMockAirQuality(125);
      expect(component.aqiQualityLabel).toBe('Dégradé');
    });

    it('should return "Mauvais" for index 151-200', () => {
      component.airQuality = createMockAirQuality(175);
      expect(component.aqiQualityLabel).toBe('Mauvais');
    });

    it('should return "Très mauvais" for index 201-300', () => {
      component.airQuality = createMockAirQuality(250);
      expect(component.aqiQualityLabel).toBe('Très mauvais');
    });

    it('should return "Extrêmement mauvais" for index >300', () => {
      component.airQuality = createMockAirQuality(350);
      expect(component.aqiQualityLabel).toBe('Extrêmement mauvais');
    });

    it('should return "Inconnu" for null data', () => {
      component.airQuality = null;
      expect(component.aqiQualityLabel).toBe('Inconnu');
    });
  });

  describe('ARIA Label', () => {
    it('should generate proper ARIA label with value and quality', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.aqiAriaLabel).toBe("Indice de qualité de l'air: 45, Bon");
    });

    it('should handle different quality levels in ARIA label', () => {
      component.airQuality = createMockAirQuality(175);
      expect(component.aqiAriaLabel).toBe("Indice de qualité de l'air: 175, Mauvais");
    });

    it('should handle no data in ARIA label', () => {
      component.airQuality = null;
      expect(component.aqiAriaLabel).toBe("Indice de qualité de l'air: —, Inconnu");
    });
  });

  describe('Data Source', () => {
    it('should return "Non disponible" when no data', () => {
      component.airQuality = null;
      expect(component.dataSourceText).toBe('Non disponible');
    });

    it('should return "Mesure directe" when data is available', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.dataSourceText).toBe('Mesure directe');
    });

    it('should return "warning" icon when no data', () => {
      component.airQuality = null;
      expect(component.dataSourceIcon).toBe('warning');
    });

    it('should return "sensors" icon when data is available', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.dataSourceIcon).toBe('sensors');
    });
  });

  describe('PM2.5 Pollutant', () => {
    it('hasPM25 should return true when pm25 is set', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.hasPM25).toBe(true);
    });

    it('hasPM25 should return false when pm25 is null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.pm25 = null as any;
      component.airQuality = airQuality;
      expect(component.hasPM25).toBe(false);
    });

    it('displayPM25 should return value as string', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.displayPM25).toBe('12');
    });

    it('displayPM25 should return "N/A" when null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.pm25 = null as any;
      component.airQuality = airQuality;
      expect(component.displayPM25).toBe('N/A');
    });

    it('should handle zero PM2.5 value', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.pm25 = 0;
      component.airQuality = airQuality;
      expect(component.displayPM25).toBe('0');
    });
  });

  describe('PM10 Pollutant', () => {
    it('hasPM10 should return true when pm10 is set', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.hasPM10).toBe(true);
    });

    it('hasPM10 should return false when pm10 is null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.pm10 = null as any;
      component.airQuality = airQuality;
      expect(component.hasPM10).toBe(false);
    });

    it('displayPM10 should return value as string', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.displayPM10).toBe('20');
    });

    it('displayPM10 should return "N/A" when null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.pm10 = null as any;
      component.airQuality = airQuality;
      expect(component.displayPM10).toBe('N/A');
    });
  });

  describe('NO2 Pollutant', () => {
    it('hasNO2 should return true when no2 is set', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.hasNO2).toBe(true);
    });

    it('hasNO2 should return false when no2 is null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.no2 = null as any;
      component.airQuality = airQuality;
      expect(component.hasNO2).toBe(false);
    });

    it('displayNO2 should return value as string', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.displayNO2).toBe('18');
    });

    it('displayNO2 should return "N/A" when null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.no2 = null as any;
      component.airQuality = airQuality;
      expect(component.displayNO2).toBe('N/A');
    });
  });

  describe('O3 Pollutant', () => {
    it('hasO3 should return true when o3 is set', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.hasO3).toBe(true);
    });

    it('hasO3 should return false when o3 is null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.o3 = null as any;
      component.airQuality = airQuality;
      expect(component.hasO3).toBe(false);
    });

    it('displayO3 should return value as string', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.displayO3).toBe('35');
    });

    it('displayO3 should return "N/A" when null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.o3 = null as any;
      component.airQuality = airQuality;
      expect(component.displayO3).toBe('N/A');
    });
  });

  describe('SO2 Pollutant', () => {
    it('hasSO2 should return true when so2 is set', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.hasSO2).toBe(true);
    });

    it('hasSO2 should return false when so2 is null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.so2 = null as any;
      component.airQuality = airQuality;
      expect(component.hasSO2).toBe(false);
    });

    it('displaySO2 should return value as string', () => {
      component.airQuality = createMockAirQuality(45);
      expect(component.displaySO2).toBe('5');
    });

    it('displaySO2 should return "N/A" when null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.so2 = null as any;
      component.airQuality = airQuality;
      expect(component.displaySO2).toBe('N/A');
    });
  });

  describe('Edge Cases', () => {
    it('should handle all pollutants being null', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.pm25 = null as any;
      airQuality.pm10 = null as any;
      airQuality.no2 = null as any;
      airQuality.o3 = null as any;
      airQuality.so2 = null as any;
      component.airQuality = airQuality;

      expect(component.hasPM25).toBe(false);
      expect(component.hasPM10).toBe(false);
      expect(component.hasNO2).toBe(false);
      expect(component.hasO3).toBe(false);
      expect(component.hasSO2).toBe(false);
    });

    it('should handle partial pollutant data', () => {
      const airQuality = createMockAirQuality(45);
      airQuality.pm25 = 12;
      airQuality.pm10 = null as any;
      airQuality.no2 = 18;
      airQuality.o3 = null as any;
      airQuality.so2 = 5;
      component.airQuality = airQuality;

      expect(component.hasPM25).toBe(true);
      expect(component.hasPM10).toBe(false);
      expect(component.hasNO2).toBe(true);
      expect(component.hasO3).toBe(false);
      expect(component.hasSO2).toBe(true);
    });

    it('should handle undefined airQuality gracefully', () => {
      component.airQuality = undefined as any;

      expect(component.hasData).toBe(false);
      expect(component.displayAqiValue).toBe('—');
      expect(component.aqiQualityLabel).toBe('Inconnu');
    });
  });

  describe('Input Changes', () => {
    it('should react to airQuality input changes', () => {
      component.airQuality = createMockAirQuality(45);
      fixture.detectChanges();
      expect(component.aqiBgClass).toBe('aqi-bg-good');

      component.airQuality = createMockAirQuality(175);
      fixture.detectChanges();
      expect(component.aqiBgClass).toBe('aqi-bg-unhealthy');
    });

    it('should react to airQuality being set to null', () => {
      component.airQuality = createMockAirQuality(45);
      fixture.detectChanges();
      expect(component.hasData).toBe(true);

      component.airQuality = null;
      fixture.detectChanges();
      expect(component.hasData).toBe(false);
    });
  });
});
