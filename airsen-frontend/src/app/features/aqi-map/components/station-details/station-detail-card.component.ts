import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Station, getAqiColor, getAqiLabel, getHealthRecommendation } from '../../models/station.model';

@Component({
  standalone: false,
  selector: 'app-station-detail-card',
  templateUrl: './station-detail-card.component.html',
  styleUrls: ['./station-detail-card.component.scss']
})
export class StationDetailCardComponent {
  @Input() station: Station | null = null;
  @Output() close = new EventEmitter<void>();

  getAqiColor = getAqiColor;
  getAqiLabel = getAqiLabel;
  getHealthRecommendation = getHealthRecommendation;

  get pollutantsList() {
    if (!this.station?.pollutants) return [];

    return [
      { key: 'pm25', label: 'PM2.5', data: this.station.pollutants.pm25 },
      { key: 'pm10', label: 'PM10', data: this.station.pollutants.pm10 },
      { key: 'o3', label: 'O₃', data: this.station.pollutants.o3 },
      { key: 'no2', label: 'NO₂', data: this.station.pollutants.no2 },
      { key: 'so2', label: 'SO₂', data: this.station.pollutants.so2 },
      { key: 'co', label: 'CO', data: this.station.pollutants.co }
    ].filter(p => p.data);
  }

  onClose(): void {
    this.close.emit();
  }

  getPollutantPercentage(value: number, max: number = 200): number {
    return Math.min((value / max) * 100, 100);
  }
}
