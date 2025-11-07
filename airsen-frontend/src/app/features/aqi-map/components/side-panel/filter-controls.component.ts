import { Component, Input, Output, EventEmitter } from '@angular/core';
import { PollutantType, TimeRange, getPollutantLabel, getTimeRangeLabel } from '../../models/map-filter.model';

@Component({
  standalone: false,
  selector: 'app-filter-controls',
  templateUrl: './filter-controls.component.html',
  styleUrls: ['./filter-controls.component.scss']
})
export class FilterControlsComponent {
  @Output() pollutantChanged = new EventEmitter<PollutantType>();
  @Output() timeRangeChanged = new EventEmitter<TimeRange>();
  @Output() heatmapToggled = new EventEmitter<boolean>();

  selectedPollutant: PollutantType = PollutantType.ALL;
  selectedTimeRange: TimeRange = TimeRange.NOW;
  showHeatmap: boolean = false;

  pollutantTypes = Object.values(PollutantType);
  timeRanges = Object.values(TimeRange);

  getPollutantLabel = getPollutantLabel;
  getTimeRangeLabel = getTimeRangeLabel;

  onPollutantChange(pollutant: PollutantType): void {
    this.selectedPollutant = pollutant;
    this.pollutantChanged.emit(pollutant);
  }

  onTimeRangeChange(timeRange: TimeRange): void {
    this.selectedTimeRange = timeRange;
    this.timeRangeChanged.emit(timeRange);
  }

  onHeatmapToggle(): void {
    this.showHeatmap = !this.showHeatmap;
    this.heatmapToggled.emit(this.showHeatmap);
  }
}
