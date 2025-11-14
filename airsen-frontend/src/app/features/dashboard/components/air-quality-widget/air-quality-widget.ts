import { Component, ChangeDetectionStrategy, Input, OnInit, OnDestroy } from "@angular/core";
import { Subject, takeUntil, catchError, of } from "rxjs";
import { AirQualityService } from "@/features/map/services/air-quality.service";

export interface AirQualityData {
  aqi: number;
  label: string;
  color: string;
  commune: string;
  timestamp: Date;
}

@Component({
  selector: "app-air-quality-widget",
  standalone: false,
  templateUrl: "./air-quality-widget.html",
  styleUrls: ["./air-quality-widget.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AirQualityWidgetComponent implements OnInit, OnDestroy {
  @Input() communeCode!: string;
  @Input() showRefreshButton = true;
  @Input() autoRefreshInterval = 0; // minutes, 0 = no auto refresh

  airQuality: AirQualityData | null = null;
  isLoading = false;
  error: string | null = null;

  private destroy$ = new Subject<void>();
  private refreshIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor(private airQualityService: AirQualityService) {}

  ngOnInit(): void {
    if (this.communeCode) {
      this.loadAirQuality();
      this.setupAutoRefresh();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    if (this.refreshIntervalId) {
      clearInterval(this.refreshIntervalId);
    }
  }

  refreshData(): void {
    this.loadAirQuality();
  }

  private loadAirQuality(): void {
    if (!this.communeCode) {
      return;
    }

    this.isLoading = true;
    this.error = null;

    this.airQualityService
      .getAirLatestQuality(this.communeCode)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          console.error("Error loading air quality data:", error);
          this.error = "Échec du chargement des données de qualité de l'air";
          this.isLoading = false;
          return of(null);
        })
      )
      .subscribe({
        next: (response: any) => {
          if (response) {
            this.airQuality = this.processAirQualityData(response);
          }
          this.isLoading = false;
        },
      });
  }

  private processAirQualityData(response: any): AirQualityData {
    return {
      aqi: response.globalIndex || response.aqi || 0,
      label: response.globalQuality || response.aqiLabel || "Unknown",
      color: response.color || this.getAqiColor(response.globalIndex || response.aqi || 0),
      commune: response.commune || "Unknown",
      timestamp: new Date(response.timestamp || response.measurementDate || Date.now()),
    };
  }

  private getAqiColor(aqi: number): string {
    if (aqi <= 50) return "#00E400"; // Green - Good
    if (aqi <= 100) return "#FFFF00"; // Yellow - Moderate
    if (aqi <= 150) return "#FF7E00"; // Orange - Unhealthy for Sensitive Groups
    if (aqi <= 200) return "#FF0000"; // Red - Unhealthy
    if (aqi <= 300) return "#8F3F97"; // Purple - Very Unhealthy
    return "#7E0023"; // Maroon - Hazardous
  }

  private setupAutoRefresh(): void {
    if (this.autoRefreshInterval > 0) {
      const intervalMs = this.autoRefreshInterval * 60 * 1000;
      this.refreshIntervalId = setInterval(() => {
        this.loadAirQuality();
      }, intervalMs);
    }
  }

  get aqiColor(): string {
    return this.airQuality?.color || "#CCCCCC";
  }

  get aqiLabel(): string {
    return this.airQuality?.label || "N/D";
  }

  get aqiValue(): number {
    return this.airQuality?.aqi || 0;
  }

  get communeName(): string {
    return this.airQuality?.commune || "Inconnu";
  }

  get lastUpdated(): Date {
    return this.airQuality?.timestamp || new Date();
  }
}
