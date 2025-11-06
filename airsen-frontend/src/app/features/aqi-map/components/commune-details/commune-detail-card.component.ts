import { Component, Input, Output, EventEmitter } from "@angular/core";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";
import { getHealthRecommendation } from "@/shared/models/air-quality.model";

@Component({
  standalone: false,
  selector: "app-commune-detail-card",
  templateUrl: "./commune-detail-card.component.html",
  styleUrls: ["./commune-detail-card.component.scss"],
})
export class CommuneDetailCardComponent {
  @Input() commune: CommuneWithAirQuality | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() addToFavorites = new EventEmitter<void>();
  @Output() exportPDF = new EventEmitter<void>();

  getHealthRecommendation = getHealthRecommendation;

  get aqiColor(): string {
    return this.commune?.currentAirQuality?.color || "#999999";
  }

  get aqiLabel(): string {
    return this.commune?.currentAirQuality?.qualifier || "Inconnu";
  }

  get atmoIndex(): number {
    return this.commune?.currentAirQuality?.atmoIndex || 0;
  }

  get pollutantsList() {
    if (!this.commune) return [];

    return [
      { key: "pm25", label: "PM2.5", data: this.commune.pollutants.pm25 },
      { key: "pm10", label: "PM10", data: this.commune.pollutants.pm10 },
      { key: "o3", label: "O₃", data: this.commune.pollutants.o3 },
      { key: "no2", label: "NO₂", data: this.commune.pollutants.no2 },
      { key: "so2", label: "SO₂", data: this.commune.pollutants.so2 },
      { key: "co", label: "CO", data: this.commune.pollutants.co },
    ].filter((p) => p.data);
  }

  onClose(): void {
    this.close.emit();
  }

  onAddToFavorites(): void {
    this.addToFavorites.emit();
  }

  onExportPDF(): void {
    this.exportPDF.emit();
  }

  getPollutantPercentage(value: number, max: number = 200): number {
    return Math.min((value / max) * 100, 100);
  }
}
