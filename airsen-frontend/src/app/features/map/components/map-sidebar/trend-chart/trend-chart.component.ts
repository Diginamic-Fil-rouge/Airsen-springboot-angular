import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from "@angular/core";
import { BaseChartDirective } from "ng2-charts";
import {
  Chart,
  ChartConfiguration,
  ChartData,
  ChartType,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  LineController,
  Filler,
  Tooltip,
  Legend,
} from "chart.js";
import { HistoricalDataService, HistoricalDataResponse, DataPoint } from "@/core/services/historical-data.service";

Chart.register(CategoryScale, LinearScale, PointElement, LineElement, LineController, Filler, Tooltip, Legend);

export type ChartMode = "aqi" | "temperature";

@Component({
  standalone: false,
  selector: "app-trend-chart",
  templateUrl: "./trend-chart.component.html",
  styleUrls: ["./trend-chart.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrendChartComponent implements OnChanges, AfterViewInit, OnDestroy {
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  @Input() inseeCode: string | null = null;
  @Input() mode: ChartMode = "aqi";

  public lineChartData: ChartData<"line"> = {
    labels: [],
    datasets: [],
  };

  public lineChartOptions: ChartConfiguration["options"] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        mode: "index",
        intersect: false,
        backgroundColor: "rgba(0, 0, 0, 0.8)",
        titleColor: "#fff",
        bodyColor: "#fff",
        borderColor: "#fff",
        borderWidth: 1,
        callbacks: {
          label: (context) => {
            const label = context.dataset.label || "";
            const value = context.parsed.y;
            if (value == null) return `${label}: N/A`;
            const unit = this.mode === "aqi" ? " AQI" : "°C";
            const formattedValue = this.formatValue(value);
            return `${label}: ${formattedValue}${unit}`;
          },
        },
      },
    },
    scales: {
      x: {
        display: true,
        grid: {
          display: false,
        },
        ticks: {
          maxRotation: 45,
          minRotation: 45,
          font: {
            size: 10,
          },
        },
      },
      y: {
        display: true,
        beginAtZero: false,
        grid: {
          color: "rgba(0, 0, 0, 0.05)",
        },
        ticks: {
          callback: (value) => {
            const unit = this.mode === "aqi" ? "" : "°C";
            const formattedValue = this.formatValue(value as number);
            return `${formattedValue}${unit}`;
          },
        },
      },
    },
  };

  public lineChartType: ChartType = "line";

  public isLoading = false;
  public hasError = false;
  public errorMessage = "";

  constructor(private historicalDataService: HistoricalDataService, private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes["inseeCode"] || changes["mode"]) && this.inseeCode) {
      this.loadChartData();
    }
  }

  ngAfterViewInit(): void {
    if (this.inseeCode) {
      this.loadChartData();
    }
  }

  ngOnDestroy(): void {
    if (this.chart?.chart) {
      this.chart.chart.destroy();
    }
  }

  /**
   * Format numeric value for display.
   * Always rounds to whole numbers (e.g., 0, 10, 14, 4)
   * No decimal values for both temperature and AQI
   */
  private formatValue(value: number): string {
    return Math.round(value).toString();
  }

  private loadChartData(): void {
    // Static fallback: Use mock data when no inseeCode provided
    if (!this.inseeCode) {
      this.buildStaticFallbackChart();
      return;
    }

    this.isLoading = true;
    this.hasError = false;
    this.cdr.markForCheck();

    const endDate = new Date();
    const startDate = new Date(endDate.getTime() - 24 * 60 * 60 * 1000);

    this.historicalDataService
      .getHistoricalData(this.inseeCode, this.formatDate(startDate), this.formatDate(endDate))
      .subscribe({
        next: (data) => {
          this.isLoading = false;
          if (this.mode === "aqi") {
            this.buildAqiChart(data);
          } else {
            this.buildTemperatureChart(data);
          }
          this.chart?.update();
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.isLoading = false;
          // Fallback to static data on error
          console.warn("[TrendChart] Data load error, using fallback data:", error);
          this.buildStaticFallbackChart();
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Build chart with static fallback data for demo
   * Paris example: 24 hours of good air quality with realistic variations
   */
  private buildStaticFallbackChart(): void {
    const labels: string[] = [];
    const values: number[] = [];
    const currentHour = new Date().getHours();

    // Generate 24 hours of data (hourly)
    for (let i = 23; i >= 0; i--) {
      const hour = (currentHour - i + 24) % 24;
      labels.push(`${hour.toString().padStart(2, '0')}h`);

      if (this.mode === "aqi") {
        // AQI values between 1-3 (Good to Moderate) with realistic pattern
        // Morning peak (7-9h): higher values, Afternoon: lower values
        const baseValue = 2;
        const variation = Math.sin((hour - 8) * Math.PI / 12) * 0.5; // Peak at 8h
        values.push(Math.max(1, Math.min(3, Math.round(baseValue + variation))));
      } else {
        // Temperature values 10-18°C with realistic pattern
        // Coldest at 6h, warmest at 15h
        const baseTemp = 14;
        const variation = 4 * Math.sin((hour - 6) * Math.PI / 12); // Peak at 15h (6 + 9)
        values.push(Math.round(baseTemp + variation));
      }
    }

    this.lineChartData = {
      labels,
      datasets: [
        {
          data: values,
          label: this.mode === "aqi" ? "Indice AQI" : "Température",
          borderColor: this.mode === "aqi" ? "#4CAF50" : "#2196F3",
          backgroundColor: this.mode === "aqi" ? "rgba(76, 175, 80, 0.1)" : "rgba(33, 150, 243, 0.1)",
          pointBackgroundColor: this.mode === "aqi" ? "#4CAF50" : "#2196F3",
          pointBorderColor: "#fff",
          pointHoverBackgroundColor: "#fff",
          pointHoverBorderColor: this.mode === "aqi" ? "#4CAF50" : "#2196F3",
          fill: true,
          tension: 0.4,
        },
      ],
    };

    if (this.chart) {
      this.chart.update();
    }
  }

  private buildAqiChart(data: HistoricalDataResponse): void {
    const labels: string[] = [];
    const aqiValues: number[] = [];

    data.dataPoints.forEach((point) => {
      if (point.airQuality?.aqi != null) {
        labels.push(this.formatTimeLabel(point.timestamp));
        aqiValues.push(point.airQuality.aqi);
      }
    });

    this.lineChartData = {
      labels,
      datasets: [
        {
          data: aqiValues,
          label: "Indice AQI",
          borderColor: "#50CCAA",
          backgroundColor: "rgba(80, 204, 170, 0.1)",
          pointBackgroundColor: "#50CCAA",
          pointBorderColor: "#fff",
          pointHoverBackgroundColor: "#fff",
          pointHoverBorderColor: "#50CCAA",
          fill: true,
          tension: 0.4,
        },
      ],
    };
  }

  private buildTemperatureChart(data: HistoricalDataResponse): void {
    const labels: string[] = [];
    const tempValues: number[] = [];

    data.dataPoints.forEach((point) => {
      if (point.weather?.temperature != null) {
        labels.push(this.formatTimeLabel(point.timestamp));
        tempValues.push(point.weather.temperature);
      }
    });

    this.lineChartData = {
      labels,
      datasets: [
        {
          data: tempValues,
          label: "Température",
          borderColor: "#FF7043",
          backgroundColor: "rgba(255, 112, 67, 0.1)",
          pointBackgroundColor: "#FF7043",
          pointBorderColor: "#fff",
          pointHoverBackgroundColor: "#fff",
          pointHoverBorderColor: "#FF7043",
          fill: true,
          tension: 0.4,
        },
      ],
    };
  }

  private formatDate(date: Date): string {
    return date.toISOString().split("T")[0];
  }

  private formatTimeLabel(timestamp: string): string {
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, "0");
    const minutes = date.getMinutes().toString().padStart(2, "0");
    return `${hours}:${minutes}`;
  }

  onModeChange(): void {
    this.loadChartData();
  }

  retryLoad(): void {
    this.loadChartData();
  }
}
