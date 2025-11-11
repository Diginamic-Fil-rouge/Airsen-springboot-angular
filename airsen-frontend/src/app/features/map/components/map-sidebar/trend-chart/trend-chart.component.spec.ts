import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TrendChartComponent } from "./trend-chart.component";
import { HistoricalDataService } from "../../../../../core/services/historical-data.service";
import { MaterialModule } from "../../../../../shared/material/material.module";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { BaseChartDirective } from "ng2-charts";
import { of, throwError } from "rxjs";
import { HistoricalDataResponse } from "../../../../../core/models/air-quality.model";
import { DebugElement } from "@angular/core";
import { By } from "@angular/platform-browser";

describe("TrendChartComponent", () => {
  let component: TrendChartComponent;
  let fixture: ComponentFixture<TrendChartComponent>;
  let historicalDataService: jasmine.SpyObj<HistoricalDataService>;
  let compiled: DebugElement;

  const mockHistoricalData: HistoricalDataResponse = {
    inseeCode: "75056",
    communeName: "Paris",
    data: [
      {
        hour: "00:00",
        aqi: 45,
        pm25: 12,
        pm10: 18,
        so2: 5,
        no2: 22,
        o3: 65,
        temperature: 15.2,
        humidity: 68,
        windSpeed: 8,
      },
      {
        hour: "01:00",
        aqi: 48,
        pm25: 14,
        pm10: 20,
        so2: 6,
        no2: 24,
        o3: 62,
        temperature: 14.8,
        humidity: 70,
        windSpeed: 7,
      },
      {
        hour: "02:00",
        aqi: 52,
        pm25: 16,
        pm10: 22,
        so2: 7,
        no2: 26,
        o3: 60,
        temperature: 14.5,
        humidity: 72,
        windSpeed: 6,
      },
    ],
  };

  beforeEach(async () => {
    const historicalDataServiceSpy = jasmine.createSpyObj("HistoricalDataService", ["getHistoricalData"]);

    await TestBed.configureTestingModule({
      imports: [TrendChartComponent, MaterialModule, BrowserAnimationsModule, BaseChartDirective],
      providers: [{ provide: HistoricalDataService, useValue: historicalDataServiceSpy }],
    }).compileComponents();

    historicalDataService = TestBed.inject(HistoricalDataService) as jasmine.SpyObj<HistoricalDataService>;
    fixture = TestBed.createComponent(TrendChartComponent);
    component = fixture.componentInstance;
    compiled = fixture.debugElement;
  });

  it("should create the component", () => {
    expect(component).toBeTruthy();
  });

  describe("Component initialization", () => {
    it("should initialize with default properties", () => {
      expect(component.inseeCode).toBeUndefined();
      expect(component.chartMode).toBe("aqi");
      expect(component.isLoading).toBe(false);
      expect(component.hasError).toBe(false);
      expect(component.chartData).toBeUndefined();
    });

    it("should not load data if inseeCode is not provided", () => {
      fixture.detectChanges();
      expect(historicalDataService.getHistoricalData).not.toHaveBeenCalled();
    });
  });

  describe("Data loading", () => {
    beforeEach(() => {
      component.inseeCode = "75056";
    });

    it("should load historical data when inseeCode changes", () => {
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));

      component.ngOnChanges({
        inseeCode: {
          currentValue: "75056",
          previousValue: undefined,
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(historicalDataService.getHistoricalData).toHaveBeenCalled();
      expect(component.isLoading).toBe(false);
      expect(component.hasError).toBe(false);
      expect(component.chartData).toBeDefined();
    });

    it("should set loading state while fetching data", () => {
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));

      component.loadHistoricalData();

      expect(component.isLoading).toBe(false);
    });

    it("should handle API errors gracefully", () => {
      const errorResponse = new Error("API Error");
      historicalDataService.getHistoricalData.and.returnValue(throwError(() => errorResponse));

      component.loadHistoricalData();

      expect(component.hasError).toBe(true);
      expect(component.isLoading).toBe(false);
      expect(component.chartData).toBeUndefined();
    });

    it("should not reload data if inseeCode has not changed", () => {
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));

      component.ngOnChanges({
        inseeCode: {
          currentValue: "75056",
          previousValue: "75056",
          firstChange: false,
          isFirstChange: () => false,
        },
      });

      expect(historicalDataService.getHistoricalData).not.toHaveBeenCalled();
    });
  });

  describe("Chart mode switching", () => {
    beforeEach(() => {
      component.inseeCode = "75056";
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      component.loadHistoricalData();
    });

    it("should update chart when mode switches to temperature", () => {
      component.onModeChange("temperature");

      expect(component.chartMode).toBe("temperature");
      expect(component.chartData).toBeDefined();
      expect(component.chartData!.datasets[0].label).toBe("Temperature (°C)");
    });

    it("should update chart when mode switches to aqi", () => {
      component.chartMode = "temperature";
      component.onModeChange("aqi");

      expect(component.chartMode).toBe("aqi");
      expect(component.chartData!.datasets[0].label).toBe("Air Quality Index");
    });

    it("should not update chart if mode is already selected", () => {
      const initialChartData = component.chartData;
      component.onModeChange("aqi");

      expect(component.chartData).toBe(initialChartData);
    });
  });

  describe("Chart data transformation", () => {
    beforeEach(() => {
      component.inseeCode = "75056";
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      component.loadHistoricalData();
    });

    it("should create AQI chart with correct data points", () => {
      const chartData = component.chartData!;

      expect(chartData.labels).toEqual(["00:00", "01:00", "02:00"]);
      expect(chartData.datasets[0].data).toEqual([45, 48, 52]);
      expect(chartData.datasets[0].label).toBe("Air Quality Index");
    });

    it("should create temperature chart with correct data points", () => {
      component.onModeChange("temperature");
      const chartData = component.chartData!;

      expect(chartData.labels).toEqual(["00:00", "01:00", "02:00"]);
      expect(chartData.datasets[0].data).toEqual([15.2, 14.8, 14.5]);
      expect(chartData.datasets[0].label).toBe("Temperature (°C)");
    });

    it("should apply correct styling for AQI chart", () => {
      const dataset = component.chartData!.datasets[0];

      expect(dataset.borderColor).toBe("rgb(46, 125, 50)");
      expect(dataset.backgroundColor).toBe("rgba(46, 125, 50, 0.1)");
      expect(dataset.fill).toBe(true);
    });

    it("should apply correct styling for temperature chart", () => {
      component.onModeChange("temperature");
      const dataset = component.chartData!.datasets[0];

      expect(dataset.borderColor).toBe("rgb(33, 150, 243)");
      expect(dataset.backgroundColor).toBe("rgba(33, 150, 243, 0.1)");
      expect(dataset.fill).toBe(true);
    });
  });

  describe("Chart options", () => {
    beforeEach(() => {
      component.inseeCode = "75056";
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      component.loadHistoricalData();
    });

    it("should configure responsive chart", () => {
      expect(component.chartOptions.responsive).toBe(true);
      expect(component.chartOptions.maintainAspectRatio).toBe(false);
    });

    it("should hide legend", () => {
      expect(component.chartOptions.plugins?.legend?.display).toBe(false);
    });

    it("should configure tooltip with custom formatting", () => {
      const tooltip = component.chartOptions.plugins?.tooltip;
      expect(tooltip?.enabled).toBe(true);
      expect(tooltip?.callbacks).toBeDefined();
    });

    it("should format AQI tooltips correctly", () => {
      const labelCallback = component.chartOptions.plugins?.tooltip?.callbacks?.label;
      if (labelCallback) {
        const mockContext = {
          parsed: { y: 45 },
          dataset: { label: "Air Quality Index" },
        };
        const result = labelCallback(mockContext as any);
        expect(result).toBe("AQI: 45");
      }
    });

    it("should format temperature tooltips correctly", () => {
      component.onModeChange("temperature");
      const labelCallback = component.chartOptions.plugins?.tooltip?.callbacks?.label;
      if (labelCallback) {
        const mockContext = {
          parsed: { y: 15.2 },
          dataset: { label: "Temperature (°C)" },
        };
        const result = labelCallback(mockContext as any);
        expect(result).toBe("Temperature: 15.2°C");
      }
    });
  });

  describe("Template rendering", () => {
    it("should show loading state when data is loading", () => {
      component.isLoading = true;
      fixture.detectChanges();

      const loadingElement = compiled.query(By.css(".loading-state"));
      expect(loadingElement).toBeTruthy();
      expect(loadingElement.nativeElement.textContent).toContain("Loading trend data");
    });

    it("should show error state when there is an error", () => {
      component.hasError = true;
      fixture.detectChanges();

      const errorElement = compiled.query(By.css(".error-state"));
      expect(errorElement).toBeTruthy();
      expect(errorElement.nativeElement.textContent).toContain("Unable to load trend data");
    });

    it("should show no data state when chartData is undefined", () => {
      component.isLoading = false;
      component.hasError = false;
      component.chartData = undefined;
      fixture.detectChanges();

      const noDataElement = compiled.query(By.css(".no-data-state"));
      expect(noDataElement).toBeTruthy();
    });

    it("should render chart when data is available", () => {
      component.inseeCode = "75056";
      component.isLoading = false;
      component.hasError = false;
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      component.loadHistoricalData();
      fixture.detectChanges();

      const canvasElement = compiled.query(By.css("canvas"));
      expect(canvasElement).toBeTruthy();
    });

    it("should render mode toggle buttons", () => {
      component.inseeCode = "75056";
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      component.loadHistoricalData();
      fixture.detectChanges();

      const toggleGroup = compiled.query(By.css("mat-button-toggle-group"));
      expect(toggleGroup).toBeTruthy();

      const aqiButton = compiled.query(By.css('mat-button-toggle[value="aqi"]'));
      const tempButton = compiled.query(By.css('mat-button-toggle[value="temperature"]'));

      expect(aqiButton).toBeTruthy();
      expect(tempButton).toBeTruthy();
    });
  });

  describe("Edge cases", () => {
    it("should handle empty historical data array", () => {
      const emptyData: HistoricalDataResponse = {
        inseeCode: "75056",
        communeName: "Paris",
        data: [],
      };

      historicalDataService.getHistoricalData.and.returnValue(of(emptyData));
      component.inseeCode = "75056";
      component.loadHistoricalData();

      expect(component.chartData).toBeDefined();
      expect(component.chartData!.labels).toEqual([]);
      expect(component.chartData!.datasets[0].data).toEqual([]);
    });

    it("should handle null values in historical data", () => {
      const dataWithNulls: HistoricalDataResponse = {
        inseeCode: "75056",
        communeName: "Paris",
        data: [
          {
            hour: "00:00",
            aqi: 45,
            pm25: 12,
            pm10: 18,
            so2: 5,
            no2: 22,
            o3: 65,
            temperature: null as any,
            humidity: 68,
            windSpeed: 8,
          },
          {
            hour: "01:00",
            aqi: null as any,
            pm25: 14,
            pm10: 20,
            so2: 6,
            no2: 24,
            o3: 62,
            temperature: 14.8,
            humidity: 70,
            windSpeed: 7,
          },
        ],
      };

      historicalDataService.getHistoricalData.and.returnValue(of(dataWithNulls));
      component.inseeCode = "75056";
      component.loadHistoricalData();

      expect(component.chartData).toBeDefined();
    });

    it("should update chart when inseeCode changes from one commune to another", () => {
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));

      component.ngOnChanges({
        inseeCode: {
          currentValue: "75056",
          previousValue: "69123",
          firstChange: false,
          isFirstChange: () => false,
        },
      });

      expect(historicalDataService.getHistoricalData).toHaveBeenCalled();
    });
  });
});
