import {
  AfterViewInit,
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions, ChartType } from 'chart.js';
import { formatDate } from '@angular/common';

export interface AirQualityTrend {
  /** ISO string or value accepted by the Date constructor. */
  date: string | number | Date;
  /** AQI value for the given date between 0 and 500. */
  value: number;
}

@Component({
  selector: 'app-air-quality-chart',
  standalone: false,
  templateUrl: './air-quality-chart.component.html',
  styleUrls: ['./air-quality-chart.component.scss'],
})
export class AirQualityChartComponent implements OnChanges, AfterViewInit {
  @Input() aqiData: AirQualityTrend[] = [];
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  readonly chartType: ChartType = 'line';

  chartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        label: "Indice de qualité de l'air (AQI)",
        data: [],
        fill: true,
        borderWidth: 2,
        tension: 0.3,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#ffffff',
        pointBorderColor: '#1f2937',
        pointBorderWidth: 1,
        backgroundColor: 'rgba(45,106,79,0.2)',
        borderColor: '#2d6a4f',
        segment: {
          borderColor: ctx => this.resolveZoneColor(ctx.p0.parsed.y ?? 0),
        },
      },
    ],
  };

  chartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 800,
      easing: 'easeInOutQuart',
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: context => `AQI : ${context.formattedValue}`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { maxRotation: 0 },
      },
      y: {
        min: 0,
        max: 500,
        ticks: { stepSize: 50 },
        grid: { color: 'rgba(148,163,184,0.2)' },
      },
    },
  };

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aqiData']) {
      this.rebuildDataset();
    }
  }

  ngAfterViewInit(): void {
    this.refreshGradient();
  }

  private rebuildDataset(): void {
    const labels = this.aqiData.map(item => formatDate(item.date, 'dd/MM', 'fr-FR'));
    const data = this.aqiData.map(item => item.value);

    this.chartData = {
      ...this.chartData,
      labels,
      datasets: this.chartData.datasets.map(dataset => ({
        ...dataset,
        data,
      })),
    };

    this.refreshGradient();
    this.chart?.update();
  }

  private refreshGradient(): void {
    const canvas = this.chart?.chart?.canvas;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) {
      return;
    }

    const gradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
    gradient.addColorStop(0, 'rgba(220,38,38,0.25)');
    gradient.addColorStop(0.3, 'rgba(249,115,22,0.2)');
    gradient.addColorStop(0.6, 'rgba(245,158,11,0.2)');
    gradient.addColorStop(1, 'rgba(45,106,79,0.25)');

    this.chartData = {
      ...this.chartData,
      datasets: this.chartData.datasets.map(dataset => ({
        ...dataset,
        backgroundColor: gradient,
      })),
    };

    this.chart?.update();
  }

  private resolveZoneColor(value: number): string {
    if (value <= 50) return '#2d6a4f';
    if (value <= 100) return '#f59e0b';
    if (value <= 150) return '#f97316';
    return '#dc2626';
  }
}
