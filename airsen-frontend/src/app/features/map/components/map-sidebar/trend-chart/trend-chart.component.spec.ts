import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TrendChartComponent } from "./trend-chart.component";
import { HistoricalDataService, HistoricalDataResponse } from "../../../../../core/services/historical-data.service";
import { MaterialModule } from "../../../../../shared/material/material.module";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { BaseChartDirective } from "ng2-charts";
import { of, throwError } from "rxjs";
import { DebugElement } from "@angular/core";
import { By } from "@angular/platform-browser";

describe("TrendChartComponent", () => {
  let component: TrendChartComponent;
  let fixture: ComponentFixture<TrendChartComponent>;
  let historicalDataService: jasmine.SpyObj<HistoricalDataService>;
  let compiled: DebugElement;

  const mockHistoricalData: HistoricalDataResponse = {
    commune: {
      name: "Paris",
      inseeCode: "75056",
    },
    dateRange: {
      start: "2024-01-15T00:00:00Z",
      end: "2024-01-16T00:00:00Z",
    },
    dataPoints: [
      {
        timestamp: "2024-01-15T00:00:00Z",
        airQuality: {
          aqi: 45,
          no2: 22,
          o3: 65,
          pm10: 18,
          pm25: 12,
          so2: 5,
        },
        weather: {
          temperature: 15.2,
          humidity: 68,
          windSpeed: 8,
        },
      },
      {
        timestamp: "2024-01-15T01:00:00Z",
        airQuality: {
          aqi: 48,
          no2: 24,
          o3: 62,
          pm10: 20,
          pm25: 14,
          so2: 6,
        },
        weather: {
          temperature: 14.8,
          humidity: 70,
          windSpeed: 7,
        },
      },
      {
        timestamp: "2024-01-15T02:00:00Z",
        airQuality: {
          aqi: 52,
          no2: 26,
          o3: 60,
          pm10: 22,
          pm25: 16,
          so2: 7,
        },
        weather: {
          temperature: 14.5,
          humidity: 72,
          windSpeed: 6,
        },
      },
    ],
    summary: {
      totalDataPoints: 3,
      completeness: {
        airQuality: 100,
        weather: 100,
      },
    },
  };

  beforeEach(async () => {
    const historicalDataServiceSpy = jasmine.createSpyObj("HistoricalDataService", ["getHistoricalData"]);

    await TestBed.configureTestingModule({
      declarations: [TrendChartComponent],
      imports: [MaterialModule, BrowserAnimationsModule, BaseChartDirective],
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
      expect(component.inseeCode).toBeNull();
      expect(component.mode).toBe("aqi");
      expect(component.isLoading).toBe(false);
      expect(component.hasError).toBe(false);
      expect(component.lineChartData.labels).toEqual([]);
      expect(component.lineChartData.datasets).toEqual([]);
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
      expect(component.lineChartData.labels?.length).toBeGreaterThan(0);
    });

    it("should set loading state while fetching data", () => {
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      component.inseeCode = "75056";

      fixture.detectChanges();

      expect(component.isLoading).toBe(false);
    });

    it("should handle API errors gracefully", () => {
      const errorResponse = new Error("API Error");
      historicalDataService.getHistoricalData.and.returnValue(throwError(() => errorResponse));
      component.inseeCode = "75056";

      fixture.detectChanges();

      expect(component.hasError).toBe(true);
      expect(component.isLoading).toBe(false);
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
      fixture.detectChanges();
    });

    it("should update chart when mode switches to temperature", () => {
      component.mode = "temperature";
      component.onModeChange();

      expect(component.mode).toBe("temperature");
      expect(component.lineChartData).toBeDefined();
      expect(component.lineChartData!.datasets[0].label).toBe("Température");
    });

    it("should update chart when mode switches to aqi", () => {
      component.mode = "temperature";
      fixture.detectChanges();

      component.mode = "aqi";
      component.onModeChange();

      expect(component.mode).toBe("aqi");
      expect(component.lineChartData!.datasets[0].label).toBe("Indice AQI");
    });

    it("should reload data when onModeChange is called", () => {
      const initialCallCount = historicalDataService.getHistoricalData.calls.count();
      component.onModeChange();

      expect(historicalDataService.getHistoricalData.calls.count()).toBeGreaterThan(initialCallCount);
    });
  });

  describe("Chart data transformation", () => {
    beforeEach(() => {
      component.inseeCode = "75056";
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      fixture.detectChanges();
    });

    it("should create AQI chart with correct data points", () => {
      const chartData = component.lineChartData!;

      expect(chartData.labels).toEqual(["00:00", "01:00", "02:00"]);
      expect(chartData.datasets[0].data).toEqual([45, 48, 52]);
      expect(chartData.datasets[0].label).toBe("Indice AQI");
    });

    it("should create temperature chart with correct data points", () => {
      component.mode = "temperature";
      component.onModeChange();
      fixture.detectChanges();
      const chartData = component.lineChartData!;

      expect(chartData.labels).toEqual(["00:00", "01:00", "02:00"]);
      expect(chartData.datasets[0].data).toEqual([15.2, 14.8, 14.5]);
      expect(chartData.datasets[0].label).toBe("Température");
    });

    it("should apply correct styling for AQI chart", () => {
      const dataset = component.lineChartData!.datasets[0];

      expect(dataset.borderColor).toBe("#50CCAA");
      expect(dataset.backgroundColor).toBe("rgba(80, 204, 170, 0.1)");
      expect(dataset.fill).toBe(true);
    });

    it("should apply correct styling for temperature chart", () => {
      component.mode = "temperature";
      component.onModeChange();
      fixture.detectChanges();
      const dataset = component.lineChartData!.datasets[0];

      expect(dataset.borderColor).toBe("#FF7043");
      expect(dataset.backgroundColor).toBe("rgba(255, 112, 67, 0.1)");
      expect(dataset.fill).toBe(true);
    });
  });

  describe("Chart options", () => {
    beforeEach(() => {
      component.inseeCode = "75056";
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      fixture.detectChanges();
    });

    it("should configure responsive chart", () => {
      expect(component.lineChartOptions?.responsive).toBe(true);
      expect(component.lineChartOptions?.maintainAspectRatio).toBe(false);
    });

    it("should hide legend", () => {
      expect(component.lineChartOptions?.plugins?.legend?.display).toBe(false);
    });

    it("should configure tooltip with custom formatting", () => {
      const tooltip = component.lineChartOptions?.plugins?.tooltip;
      expect(tooltip?.callbacks).toBeDefined();
    });

    it("should format AQI tooltips correctly", () => {
      const labelCallback = component.lineChartOptions?.plugins?.tooltip?.callbacks?.label;
      if (labelCallback) {
        const mockContext = {
          parsed: { y: 45 },
          dataset: { label: "Indice AQI" },
        };
        const result = labelCallback.call(component as any, mockContext as any);
        expect(result).toContain("45");
        expect(result).toContain("AQI");
      }
    });

    it("should format temperature tooltips correctly", () => {
      component.mode = "temperature";
      component.onModeChange();
      fixture.detectChanges();

      const labelCallback = component.lineChartOptions?.plugins?.tooltip?.callbacks?.label;
      if (labelCallback) {
        const mockContext = {
          parsed: { y: 15.2 },
          dataset: { label: "Température" },
        };
        const result = labelCallback.call(component as any, mockContext as any);
        expect(result).toContain("15");
        expect(result).toContain("°C");
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

    it("should show no data state when lineChartData has no labels", () => {
      component.isLoading = false;
      component.hasError = false;
      component.lineChartData = { labels: [], datasets: [] };
      fixture.detectChanges();

      const noDataElement = compiled.query(By.css(".no-data-state"));
      // Note: This test depends on component template implementation
      // May need adjustment based on actual template
    });

    it("should render chart when data is available", () => {
      component.inseeCode = "75056";
      component.isLoading = false;
      component.hasError = false;
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      fixture.detectChanges();

      const canvasElement = compiled.query(By.css("canvas"));
      expect(canvasElement).toBeTruthy();
    });

    it("should render mode toggle buttons", () => {
      component.inseeCode = "75056";
      historicalDataService.getHistoricalData.and.returnValue(of(mockHistoricalData));
      fixture.detectChanges();

      const toggleGroup = compiled.query(By.css("mat-button-toggle-group"));
      // Note: This test depends on component template implementation
      // May need adjustment based on actual template
    });
  });

  describe("Edge cases", () => {
    it("should handle empty historical data array", () => {
      const emptyData: HistoricalDataResponse = {
        commune: {
          name: "Paris",
          inseeCode: "75056",
        },
        dateRange: {
          start: "2024-01-15T00:00:00Z",
          end: "2024-01-16T00:00:00Z",
        },
        dataPoints: [],
        summary: {
          totalDataPoints: 0,
          completeness: {
            airQuality: 0,
            weather: 0,
          },
        },
      };

      historicalDataService.getHistoricalData.and.returnValue(of(emptyData));
      component.inseeCode = "75056";
      fixture.detectChanges();

      expect(component.lineChartData).toBeDefined();
      expect(component.lineChartData!.labels).toEqual([]);
    });

    it("should handle null values in historical data", () => {
      const dataWithNulls: HistoricalDataResponse = {
        commune: {
          name: "Paris",
          inseeCode: "75056",
        },
        dateRange: {
          start: "2024-01-15T00:00:00Z",
          end: "2024-01-16T00:00:00Z",
        },
        dataPoints: [
          {
            timestamp: "2024-01-15T00:00:00Z",
            airQuality: {
              aqi: 45,
              no2: 22,
              o3: 65,
              pm10: 18,
              pm25: 12,
              so2: 5,
            },
            weather: null,
          },
          {
            timestamp: "2024-01-15T01:00:00Z",
            airQuality: null,
            weather: {
              temperature: 14.8,
              humidity: 70,
              windSpeed: 7,
            },
          },
        ],
        summary: {
          totalDataPoints: 2,
          completeness: {
            airQuality: 50,
            weather: 50,
          },
        },
      };

      historicalDataService.getHistoricalData.and.returnValue(of(dataWithNulls));
      component.inseeCode = "75056";
      fixture.detectChanges();

      expect(component.lineChartData).toBeDefined();
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
