import { ComponentFixture, TestBed } from "@angular/core/testing";
import { AirQualityWidgetComponent } from "./air-quality-widget";
import { AirQualityService } from "@/app/features/m/services/air-quality.service";
import { of, throwError } from "rxjs";
import { By } from "@angular/platform-browser";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { AqiColorPipe } from "@/shared/pipes/aqi-color.pipe";
import { AqiLabelPipe } from "@/shared/pipes/aqi-label.pipe";

describe("AirQualityWidgetComponent", () => {
  let component: AirQualityWidgetComponent;
  let fixture: ComponentFixture<AirQualityWidgetComponent>;
  let airQualityService: jasmine.SpyObj<AirQualityService>;

  const mockAirQualityData = {
    globalIndex: 75,
    globalQuality: "MODERATE",
    commune: "Paris",
    timestamp: new Date("2024-01-15T10:30:00Z"),
  };

  beforeEach(async () => {
    const airQualityServiceMock = jasmine.createSpyObj("AirQualityService", ["getAirLatestQuality"]);

    await TestBed.configureTestingModule({
      imports: [AirQualityWidgetComponent, MatIconModule, MatProgressSpinnerModule, AqiColorPipe, AqiLabelPipe],
      providers: [{ provide: AirQualityService, useValue: airQualityServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(AirQualityWidgetComponent);
    component = fixture.componentInstance;
    airQualityService = TestBed.inject(AirQualityService) as jasmine.SpyObj<AirQualityService>;
  });

  afterEach(() => {
    // Clear all spies
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should load air quality data on init", () => {
    airQualityService.getAirLatestQuality.and.returnValue(of(mockAirQualityData));
    component.communeCode = "75056";

    fixture.detectChanges(); // triggers ngOnInit

    expect(airQualityService.getAirLatestQuality).toHaveBeenCalledWith("75056");
    expect(component.airQuality).toBeTruthy();
    expect(component.isLoading).toBe(false);
  });

  it("should display air quality data when loaded", () => {
    airQualityService.getAirLatestQuality.and.returnValue(of(mockAirQualityData));
    component.communeCode = "75056";

    fixture.detectChanges(); // triggers ngOnInit

    const aqiValue = fixture.debugElement.query(By.css(".aqi-value"));
    const aqiLabel = fixture.debugElement.query(By.css(".aqi-label"));
    const communeName = fixture.debugElement.query(By.css(".commune-name"));

    expect(aqiValue.nativeElement.textContent).toBe("75");
    expect(aqiLabel.nativeElement.textContent).toBe("MODERATE");
    expect(communeName.nativeElement.textContent).toBe("Paris");
  });

  it("should handle loading state", () => {
    airQualityService.getAirLatestQuality.and.returnValue(of(mockAirQualityData));
    component.communeCode = "75056";

    fixture.detectChanges();

    expect(component.isLoading).toBe(false);

    // Trigger refresh to see loading state
    component.refreshData();
    fixture.detectChanges();

    expect(component.isLoading).toBe(true);
  });

  it("should handle error state", () => {
    airQualityService.getAirLatestQuality.and.returnValue(throwError(() => new Error("API Error")));
    component.communeCode = "75056";

    fixture.detectChanges();

    expect(component.error).toBe("Failed to load air quality data");
    expect(component.isLoading).toBe(false);
    expect(component.airQuality).toBeNull();
  });

  it("should display error message when data fails to load", () => {
    airQualityService.getAirLatestQuality.and.returnValue(throwError(() => new Error("API Error")));
    component.communeCode = "75056";

    fixture.detectChanges();

    const errorElement = fixture.debugElement.query(By.css(".error-container p"));
    expect(errorElement.nativeElement.textContent).toBe("Failed to load air quality data");
  });

  it("should retry loading data on refresh", () => {
    airQualityService.getAirLatestQuality.and.returnValue(of(mockAirQualityData));
    component.communeCode = "75056";

    fixture.detectChanges();

    expect(airQualityService.getAirLatestQuality).toHaveBeenCalledTimes(1);

    component.refreshData();
    fixture.detectChanges();

    expect(airQualityService.getAirLatestQuality).toHaveBeenCalledTimes(2);
  });

  it("should display no data message when no commune code is provided", () => {
    component.communeCode = undefined as any;

    fixture.detectChanges();

    const noDataElement = fixture.debugElement.query(By.css(".no-data-container p"));
    expect(noDataElement.nativeElement.textContent).toBe("No air quality data available");
  });

  it("should display refresh button when enabled", () => {
    airQualityService.getAirLatestQuality.and.returnValue(of(mockAirQualityData));
    component.communeCode = "75056";
    component.showRefreshButton = true;

    fixture.detectChanges();

    const refreshButton = fixture.debugElement.query(By.css(".refresh-button"));
    expect(refreshButton).toBeTruthy();
  });

  it("should hide refresh button when disabled", () => {
    airQualityService.getAirLatestQuality.and.returnValue(of(mockAirQualityData));
    component.communeCode = "75056";
    component.showRefreshButton = false;

    fixture.detectChanges();

    const refreshButton = fixture.debugElement.query(By.css(".refresh-button"));
    expect(refreshButton).toBeFalsy();
  });

  it("should set correct AQI indicator color", () => {
    airQualityService.getAirLatestQuality.and.returnValue(of(mockAirQualityData));
    component.communeCode = "75056";

    fixture.detectChanges();

    const aqiIndicator = fixture.debugElement.query(By.css(".aqi-indicator"));
    expect(aqiIndicator.nativeElement.style.backgroundColor).toBeTruthy();
  });

  it("should display AQI scale", () => {
    airQualityService.getAirLatestQuality.and.returnValue(of(mockAirQualityData));
    component.communeCode = "75056";

    fixture.detectChanges();

    const scaleItems = fixture.debugElement.queryAll(By.css(".scale-item"));
    expect(scaleItems.length).toBe(6); // Should have all 6 AQI categories
  });

  it("should handle data with different response format", () => {
    const alternativeData = {
      aqi: 85,
      aqiLabel: "Moderate",
      commune: "Lyon",
      timestamp: new Date(),
    };

    airQualityService.getAirLatestQuality.and.returnValue(of(alternativeData));
    component.communeCode = "69003";

    fixture.detectChanges();

    expect(component.airQuality).toBeTruthy();
    expect(component.aqiValue).toBe(85);
    expect(component.aqiLabel).toBe("Moderate");
  });

  it("should clean up on destroy", () => {
    const clearIntervalSpy = spyOn(window, "clearInterval");

    component.communeCode = "75056";
    component.autoRefreshInterval = 5;

    fixture.detectChanges();
    component.ngOnDestroy();

    expect(clearIntervalSpy).toHaveBeenCalled();
  });
});
