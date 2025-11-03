import {
  AfterViewInit,
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { BaseChartDirective } from 'ng2-charts';
import {
  Chart,
  ChartData,
  ChartOptions,
  ChartType,
  CategoryScale,
  LinearScale,
  BarElement,
  PointElement,
  LineElement,
  LineController,
  BarController,
  Filler,
  Tooltip,
  Legend,
  ScaleOptions,
} from 'chart.js';
import { formatDate } from '@angular/common';

// Register Chart.js components and controllers
Chart.register(
  CategoryScale,
  LinearScale,
  BarElement,
  BarController,
  PointElement,
  LineElement,
  LineController,
  Filler,
  Tooltip,
  Legend
);

export interface AirQualityTrend {
  /** ISO string or value accepted by the Date constructor. */
  date: string | number | Date;
  /** AQI value for the given date between 0 and 500. */
  value: number;
}

export interface PollutantData {
  PM10: number;
  PM2_5: number;
  O3: number;
  NO2: number;
  SO2: number;
}

// AQI Colors aligned with design-system.scss
const AQI_COLORS = {
  good: '#4CAF50',           // Bon (0-50)
  moderate: '#FFC107',       // Moyen (51-100)
  sensitive: '#FF9800',      // Dégradé (101-150)
  unhealthy: '#F44336',      // Mauvais (151-200)
  veryUnhealthy: '#9C27B0',  // Très mauvais (201-300)
  hazardous: '#8D2635',      // Extrêmement mauvais (301+)
};

// Color map for gradients with opacity
const AQI_COLORS_RGBA = {
  good: 'rgba(76, 175, 80, 0.25)',           // #4CAF50
  moderate: 'rgba(255, 193, 7, 0.2)',        // #FFC107
  sensitive: 'rgba(255, 152, 0, 0.2)',       // #FF9800
  unhealthy: 'rgba(244, 67, 54, 0.25)',      // #F44336
  veryUnhealthy: 'rgba(156, 39, 176, 0.25)', // #9C27B0
  hazardous: 'rgba(141, 38, 53, 0.25)',      // #8D2635
};

@Component({
  selector: 'app-air-quality-chart',
  standalone: false,
  templateUrl: './air-quality-chart.component.html',
  styleUrls: ['./air-quality-chart.component.scss'],
})
export class AirQualityChartComponent implements OnChanges, AfterViewInit {
  @Input() aqiData: AirQualityTrend[] = [];
  @Input() pollutantData?: PollutantData;
  @Input() chartMode: 'line' | 'bar' = 'line';
  @Input() isMapDisplay: boolean = false; // New input for map-specific styling
  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  chartType: ChartType = 'line';

  chartData: ChartData = {
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
        backgroundColor: AQI_COLORS_RGBA.good,
        borderColor: AQI_COLORS.good,
        segment: {
          borderColor: ctx => this.resolveZoneColor(ctx.p0.parsed.y ?? 0),
        },
      },
    ],
  };

  chartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 800,
      easing: 'easeInOutQuart' as const,
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
    elements: {
      bar: {
        borderRadius: 8,
        borderSkipped: false,
      }
    }
  };

  ngOnChanges(changes: SimpleChanges): void {
    // Handle chart mode changes (including first change)
    if (changes['chartMode']) {
      this.chartType = this.chartMode;
      this.updateChartOptions();
      if (this.chart) {
        this.chart.update();
      }
    }

    // Handle AQI data for line mode
    if (changes['aqiData'] && this.chartMode === 'line') {
      this.rebuildLineDataset();
    }

    // Handle pollutant data for bar mode
    if (changes['pollutantData'] && this.chartMode === 'bar') {
      this.rebuildBarDataset();
    }
  }

  ngAfterViewInit(): void {
    if (this.chartMode === 'line') {
      this.chartType = 'line';
      this.refreshGradient();
    } else if (this.chartMode === 'bar') {
      this.chartType = 'bar';
      this.updateChartOptions();
      if (this.pollutantData) {
        this.rebuildBarDataset();
      }
      this.chart?.update();
    }
  }

  private rebuildLineDataset(): void {
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

  private rebuildBarDataset(): void {
    if (!this.pollutantData) return;

    const labels = ['PM10', 'PM2.5', 'O3', 'NO2', 'SO2'];
    const data = [
      this.pollutantData.PM10 || 0,
      this.pollutantData.PM2_5 || 0,
      this.pollutantData.O3 || 0,
      this.pollutantData.NO2 || 0,
      this.pollutantData.SO2 || 0,
    ];

    // Enhanced colors for better visual distinction in map display
    const backgroundColors = [
      'rgba(46, 125, 50, 0.85)',   // PM10 - Vibrant Green
      'rgba(33, 150, 243, 0.85)',  // PM2.5 - Vibrant Blue
      'rgba(255, 152, 0, 0.85)',   // O3 - Orange
      'rgba(156, 39, 176, 0.85)',  // NO2 - Purple
      'rgba(244, 67, 54, 0.85)',   // SO2 - Red
    ];

    const borderColors = [
      'rgba(46, 125, 50, 1)',
      'rgba(33, 150, 243, 1)',
      'rgba(255, 152, 0, 1)',
      'rgba(156, 39, 176, 1)',
      'rgba(244, 67, 54, 1)',
    ];

    this.chartData = {
      labels,
      datasets: [
        {
          label: 'Pollutants (μg/m³)',
          data,
          backgroundColor: backgroundColors,
          borderColor: borderColors,
          borderWidth: 0,
          borderRadius: 3, // Rounded bars instead of MAX_VALUE
          barPercentage: 0.85, // 85% of category space for fuller bars
          categoryPercentage: 0.9, // 90% of axis space
        },
      ],
    };

    this.chart?.update();
  }

  private updateChartOptions(): void {
    if (this.chartMode === 'bar') {
      const baseBarOptions = {
        indexAxis: 'y' as const,
        responsive: true,
        maintainAspectRatio: false,
        animation: {
          duration: 800,
          easing: 'easeInOutQuart' as const,
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            enabled: true,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            cornerRadius: 8,
            titleFont: { size: 14, weight: 'bold' as const },
            bodyFont: { size: 13 },
            callbacks: {
              label: (context: any) => `${context.parsed.x} μg/m³`,
            },
          },
        },
        scales: {
          x: {
            beginAtZero: true,
            max: 200,
            ticks: {
              stepSize: 50,
              color: '#64748b',
              font: { size: 11 },
            },
            grid: {
              display: true,
              color: 'rgba(148, 163, 184, 0.15)',
              lineWidth: 1,
            },
            border: { display: false },
          },
          y: {
            ticks: {
              color: '#475569',
              font: { size: 13, weight: 'normal' as const },
              padding: 8,
            },
            grid: { display: false },
            border: { display: false },
          },
        },
        layout: {
          padding: {
            top: 5,
            bottom: 5,
            left: 0,
            right: 10,
          },
        },
        elements: {
          bar: {
            borderRadius: 3, // More rounded for full-width bars
            borderSkipped: false, // Rounded on all sides
          }
        }
      };

      // Compact options for map display
      if (this.isMapDisplay) {
        this.chartOptions = {
          ...baseBarOptions,
          plugins: {
            ...baseBarOptions.plugins,
            tooltip: {
              ...baseBarOptions.plugins.tooltip,
              padding: 8,
              titleFont: { size: 12, weight: 'bold' as const },
              bodyFont: { size: 11 },
            },
          },
          scales: {
            x: {
              ...baseBarOptions.scales.x,
              max: 150, // Reduced max for map display
              ticks: {
                ...baseBarOptions.scales.x.ticks,
                font: { size: 10 },
                stepSize: 25,
              },
            },
            y: {
              ...baseBarOptions.scales.y,
              ticks: {
                ...baseBarOptions.scales.y.ticks,
                font: { size: 11, weight: 'normal' as const },
                padding: 4,
              },
            },
          },
          layout: {
            padding: {
              top: 2,
              bottom: 2,
              left: 0,
              right: 5,
            },
          },
          elements: {
            bar: {
              borderRadius: 3, // Slightly less rounded for compact display
              borderSkipped: false,
            }
          }
        };
      } else {
        this.chartOptions = baseBarOptions;
      }
    }
  }

  private refreshGradient(): void {
    const canvas = this.chart?.chart?.canvas;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) {
      return;
    }

    // Gradient based on AQI levels (design-system colors)
    // Top: Hazardous, Middle-top: Unhealthy, Middle: Sensitive, Middle-bottom: Moderate, Bottom: Good
    const gradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
    gradient.addColorStop(0, AQI_COLORS_RGBA.hazardous);        // Extrêmement mauvais (300+)
    gradient.addColorStop(0.25, AQI_COLORS_RGBA.veryUnhealthy); // Très mauvais (201-300)
    gradient.addColorStop(0.5, AQI_COLORS_RGBA.unhealthy);      // Mauvais (151-200)
    gradient.addColorStop(0.65, AQI_COLORS_RGBA.sensitive);     // Dégradé (101-150)
    gradient.addColorStop(0.8, AQI_COLORS_RGBA.moderate);       // Moyen (51-100)
    gradient.addColorStop(1, AQI_COLORS_RGBA.good);             // Bon (0-50)

    this.chartData = {
      ...this.chartData,
      datasets: this.chartData.datasets.map(dataset => ({
        ...dataset,
        backgroundColor: gradient,
      })),
    };

    this.chart?.update();
  }

  /**
   * Resolves the AQI color based on the value.
   * Aligns with design-system.scss AQI color scale and map legend markers.
   * @param value The AQI value (0-500)
   * @returns The hex color for the given AQI value
   */
  private resolveZoneColor(value: number): string {
    if (value <= 50) {
      return AQI_COLORS.good;           // Bon
    } else if (value <= 100) {
      return AQI_COLORS.moderate;       // Moyen
    } else if (value <= 150) {
      return AQI_COLORS.sensitive;      // Dégradé
    } else if (value <= 200) {
      return AQI_COLORS.unhealthy;      // Mauvais
    } else if (value <= 300) {
      return AQI_COLORS.veryUnhealthy;  // Très mauvais
    } else {
      return AQI_COLORS.hazardous;      // Extrêmement mauvais
    }
  }
}
