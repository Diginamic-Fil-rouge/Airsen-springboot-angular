import { ComponentFixture, TestBed } from "@angular/core/testing";
import { PollutantBreakdownComponent } from "./pollutant-breakdown.component";
import { BaseChartDirective } from "ng2-charts";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatChipsModule } from "@angular/material/chips";
import { MatButtonModule } from "@angular/material/button";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

describe("PollutantBreakdownComponent", () => {
  let component: PollutantBreakdownComponent;
  let fixture: ComponentFixture<PollutantBreakdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PollutantBreakdownComponent],
      imports: [
        BaseChartDirective,
        MatIconModule,
        MatTooltipModule,
        MatChipsModule,
        MatButtonModule,
        NoopAnimationsModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PollutantBreakdownComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize with null pollutants", () => {
    component.pollutants = null;
    component.ngOnChanges({
      pollutants: {
        currentValue: null,
        previousValue: undefined,
        firstChange: true,
        isFirstChange: () => true,
      },
    });
    expect(component.pollutantConfig).toBeDefined();
    expect(component.pollutantConfig.length).toBe(5);
  });

  it("should build pollutant list with provided data", () => {
    component.pollutants = {
      pm25: 15,
      pm10: 35,
      so2: 10,
      no2: 20,
      o3: 80,
    };
    component.ngOnChanges({
      pollutants: {
        currentValue: component.pollutants,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    expect(component.pollutantConfig.length).toBe(5);
    expect(component.pollutantConfig[0].name).toBe("PM2.5");
    expect(component.pollutantConfig[0].value).toBe(15);
    expect(component.pollutantConfig[1].value).toBe(35);
  });

  it("should assign correct color based on value thresholds", () => {
    component.pollutants = {
      pm25: 15,
      pm10: 60,
      so2: 30,
    };
    component.ngOnChanges({
      pollutants: {
        currentValue: component.pollutants,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    expect(component.pollutantConfig[0].color).toBe("#50F0E6"); // PM2.5: 15 <= 25 (threshold) → Good (Cyan)
    expect(component.pollutantConfig[1].color).toBe("#F0E641"); // PM10: 60 > 50 (threshold) AND 60 <= 100 (danger) → Moderate (Yellow)
    expect(component.pollutantConfig[2].color).toBe("#F0E641"); // SO2: 30 > 20 (threshold) AND 30 <= 40 (danger) → Moderate (Yellow)
  });

  it("should return correct status text", () => {
    const goodPollutant = {
      name: "PM2.5",
      value: 15,
      unit: "μg/m³",
      threshold: 25,
      dangerLevel: 50,
      color: "#50F0E6",
      icon: "grain",
      description: "Test",
    };
    expect(component.getStatusText(goodPollutant)).toBe("Bon");

    const moderatePollutant = { ...goodPollutant, value: 35 };
    expect(component.getStatusText(moderatePollutant)).toBe("Modéré");

    const elevatedPollutant = { ...goodPollutant, value: 60 };
    expect(component.getStatusText(elevatedPollutant)).toBe("Élevé");

    const nullPollutant = { ...goodPollutant, value: null };
    expect(component.getStatusText(nullPollutant)).toBe("N/A");
  });

  it("should format values correctly", () => {
    expect(component.formatValue(15.567)).toBe("15.6");
    expect(component.formatValue(null)).toBe("N/A");
    expect(component.formatValue(0)).toBe("0.0");
  });

  it("should generate chart data with correct structure", () => {
    component.pollutants = {
      pm25: 20,
      pm10: 40,
      so2: 15,
      no2: 30,
      o3: 90,
    };
    component.ngOnChanges({
      pollutants: {
        currentValue: component.pollutants,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    expect(component.chartData.labels).toEqual(["PM2.5", "PM10", "SO₂", "NO₂", "O₃"]);
    expect(component.chartData.datasets).toBeDefined();
    expect(component.chartData.datasets.length).toBe(1);
    expect(component.chartData.datasets[0].data).toEqual([20, 40, 15, 30, 90]);
  });

  it("should handle missing pollutant values", () => {
    component.pollutants = {
      pm25: 20,
    };
    component.ngOnChanges({
      pollutants: {
        currentValue: component.pollutants,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    expect(component.pollutantConfig[0].value).toBe(20);
    expect(component.pollutantConfig[1].value).toBeNull();
    expect(component.pollutantConfig[2].value).toBeNull();
    expect(component.pollutantConfig[3].value).toBeNull();
    expect(component.pollutantConfig[4].value).toBeNull();
  });

  it("should generate proper aria labels", () => {
    const pollutant = {
      name: "PM2.5",
      value: 15,
      unit: "μg/m³",
      threshold: 25,
      dangerLevel: 50,
      color: "#50F0E6",
      icon: "grain",
      description: "Test",
    };

    const ariaLabel = component.getPollutantAriaLabel(pollutant);
    expect(ariaLabel).toContain("PM2.5");
    expect(ariaLabel).toContain("15.0");
    expect(ariaLabel).toContain("μg/m³");
    expect(ariaLabel).toContain("Bon");
  });

  it("should have horizontal bar chart configuration", () => {
    expect(component.chartOptions.indexAxis).toBe("y");
    expect(component.chartOptions.responsive).toBe(true);
    expect(component.chartOptions.maintainAspectRatio).toBe(false);
  });

  it("should update chart when pollutants change", () => {
    component.pollutants = { pm25: 10 };
    component.ngOnChanges({
      pollutants: {
        currentValue: component.pollutants,
        previousValue: null,
        firstChange: true,
        isFirstChange: () => true,
      },
    });

    const initialData = component.chartData.datasets[0].data[0];

    component.pollutants = { pm25: 30 };
    component.ngOnChanges({
      pollutants: {
        currentValue: component.pollutants,
        previousValue: { pm25: 10 },
        firstChange: false,
        isFirstChange: () => false,
      },
    });

    expect(component.chartData.datasets[0].data[0]).not.toBe(initialData);
    expect(component.chartData.datasets[0].data[0]).toBe(30);
  });
});
