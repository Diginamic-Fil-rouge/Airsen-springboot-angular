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
    if (!this.inseeCode) return;

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
          this.hasError = true;
          this.errorMessage = "Impossible de charger les données historiques";
          console.error("[TrendChart] Data load error:", error);
          this.cdr.markForCheck();
        },
      });
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
