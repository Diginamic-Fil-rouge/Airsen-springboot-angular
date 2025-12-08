import {
  AfterViewInit,
  ChangeDetectionStrategy,
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
  ChartData,
  ChartOptions,
  CategoryScale,
  LinearScale,
  BarElement,
  BarController,
  Tooltip,
  Legend,
} from "chart.js";

Chart.register(CategoryScale, LinearScale, BarElement, BarController, Tooltip, Legend);

interface PollutantData {
  name: string;
  value: number | null;
  unit: string;
  threshold: number;
  dangerLevel: number;
  color: string;
  icon: string;
  description: string;
}

@Component({
  standalone: false,
  selector: "app-pollutant-breakdown",
  templateUrl: "./pollutant-breakdown.component.html",
  styleUrls: ["./pollutant-breakdown.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PollutantBreakdownComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() pollutants: {
    pm25?: number;
    pm10?: number;
    so2?: number;
    no2?: number;
    o3?: number;
  } | null = null;

  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  pollutantConfig: PollutantData[] = [];

  public chartData: ChartData<"bar"> = {
    labels: [],
    datasets: [],
  };

  public chartOptions: ChartOptions<"bar"> = {
    indexAxis: "y",
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        enabled: true,
        callbacks: {
          label: (context) => {
            const pollutant = this.pollutantConfig[context.dataIndex];
            const value = context.parsed.x;
            const status = this.getStatusText(pollutant);
            if (value == null) return "N/A";
            return `${value.toFixed(1)} ${pollutant.unit} - ${status}`;
          },
          title: (context) => {
            const pollutant = this.pollutantConfig[context[0].dataIndex];
            return pollutant.description;
          },
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        title: {
          display: true,
          text: "Concentration (μg/m³)",
        },
        grid: {
          color: "rgba(0, 0, 0, 0.05)",
        },
      },
      y: {
        grid: {
          display: false,
        },
      },
    },
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["pollutants"]) {
      this.buildPollutantList();
      this.updateChartData();
    }
  }

  ngAfterViewInit(): void {
    this.updateChartData();
  }

  ngOnDestroy(): void {
    if (this.chart?.chart) {
      this.chart.chart.destroy();
    }
  }

  private buildPollutantList(): void {
    // Static fallback data for demo when no real data is available
    // Paris example: Good air quality (values below thresholds)
    const pollutants = this.pollutants || {};
    const useFallback = !this.pollutants;

    this.pollutantConfig = [
      {
        name: "PM2.5",
        value: pollutants.pm25 ?? (useFallback ? 12.5 : null),
        unit: "μg/m³",
        threshold: 25,
        dangerLevel: 50,
        color: this.getColorForValue(pollutants.pm25 ?? (useFallback ? 12.5 : undefined), 25, 50),
        icon: "grain",
        description: "Particules fines - Sources: combustion, trafic routier",
      },
      {
        name: "PM10",
        value: pollutants.pm10 ?? (useFallback ? 18.3 : null),
        unit: "μg/m³",
        threshold: 50,
        dangerLevel: 100,
        color: this.getColorForValue(pollutants.pm10 ?? (useFallback ? 18.3 : undefined), 50, 100),
        icon: "blur_on",
        description: "Particules grossières - Sources: poussières, construction",
      },
      {
        name: "SO₂",
        value: pollutants.so2 ?? (useFallback ? 5.2 : null),
        unit: "μg/m³",
        threshold: 20,
        dangerLevel: 40,
        color: this.getColorForValue(pollutants.so2 ?? (useFallback ? 5.2 : undefined), 20, 40),
        icon: "factory",
        description: "Dioxyde de soufre - Source: industrie, chauffage",
      },
      {
        name: "NO₂",
        value: pollutants.no2 ?? (useFallback ? 15.8 : null),
        unit: "μg/m³",
        threshold: 25,
        dangerLevel: 50,
        color: this.getColorForValue(pollutants.no2 ?? (useFallback ? 15.8 : undefined), 25, 50),
        icon: "directions_car",
        description: "Dioxyde d'azote - Source: trafic routier",
      },
      {
        name: "O₃",
        value: pollutants.o3 ?? (useFallback ? 45.6 : null),
        unit: "μg/m³",
        threshold: 100,
        dangerLevel: 200,
        color: this.getColorForValue(pollutants.o3 ?? (useFallback ? 45.6 : undefined), 100, 200),
        icon: "wb_sunny",
        description: "Ozone - Formé par réaction solaire avec polluants",
      },
    ];
  }

  private updateChartData(): void {
    const labels = this.pollutantConfig.map((p) => p.name);
    const data = this.pollutantConfig.map((p) => p.value ?? 0);
    const backgroundColors = this.pollutantConfig.map((p) => p.color);
    const borderColors = this.pollutantConfig.map((p) => this.darkenColor(p.color));

    this.chartData = {
      labels,
      datasets: [
        {
          label: "Concentration",
          data,
          backgroundColor: backgroundColors,
          borderColor: borderColors,
          borderWidth: 1,
          barThickness: 24,
        },
      ],
    };

    if (this.chart?.chart) {
      this.chart.chart.update();
    }
  }

  private getColorForValue(value: number | undefined, threshold: number, danger: number): string {
    if (value == null) return "#CCCCCC";
    if (value <= threshold) return "#50F0E6";
    if (value <= danger) return "#F0E641";
    return "#FF5050";
  }

  private darkenColor(color: string): string {
    const colorMap: Record<string, string> = {
      "#50F0E6": "#3AC7BD",
      "#F0E641": "#D4C935",
      "#FF5050": "#E63D3D",
      "#CCCCCC": "#999999",
    };
    return colorMap[color] || color;
  }

  formatValue(value: number | null): string {
    if (value == null) return "N/A";
    return value.toFixed(1);
  }

  getStatusText(pollutant: PollutantData): string {
    if (pollutant.value == null) return "N/A";
    if (pollutant.value <= pollutant.threshold) return "Bon";
    if (pollutant.value <= pollutant.dangerLevel) return "Modéré";
    return "Élevé";
  }

  getPollutantAriaLabel(pollutant: PollutantData): string {
    return `${pollutant.name}: ${this.formatValue(pollutant.value)} ${pollutant.unit}, ${this.getStatusText(
      pollutant
    )}`;
  }

  trackByName(index: number, pollutant: PollutantData): string {
    return pollutant.name;
  }
}
