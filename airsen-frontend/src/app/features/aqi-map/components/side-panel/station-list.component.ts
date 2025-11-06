import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Station, getAqiColor, getAqiLabel } from '../../models/station.model';

@Component({
  standalone: false,
  selector: 'app-station-list',
  templateUrl: './station-list.component.html',
  styleUrls: ['./station-list.component.scss']
})
export class StationListComponent {
  @Input() stations: Station[] = [];
  @Input() selectedStation: Station | null = null;
  @Output() stationSelected = new EventEmitter<Station>();

  searchQuery: string = '';
  sortBy: 'name' | 'aqi' | 'population' = 'aqi';
  sortDirection: 'asc' | 'desc' = 'desc';

  getAqiColor = getAqiColor;
  getAqiLabel = getAqiLabel;

  get filteredAndSortedStations(): Station[] {
    let result = [...this.stations];

    // Filter by search query
    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      result = result.filter(s =>
        s.name.toLowerCase().includes(query) ||
        s.inseeCode.includes(query)
      );
    }

    // Sort
    result.sort((a, b) => {
      let compareValue = 0;

      switch (this.sortBy) {
        case 'name':
          compareValue = a.name.localeCompare(b.name);
          break;
        case 'aqi':
          compareValue = (a.currentAqi || 0) - (b.currentAqi || 0);
          break;
        case 'population':
          compareValue = (a.population || 0) - (b.population || 0);
          break;
      }

      return this.sortDirection === 'asc' ? compareValue : -compareValue;
    });

    return result;
  }

  onStationClick(station: Station): void {
    this.stationSelected.emit(station);
  }

  toggleSort(field: 'name' | 'aqi' | 'population'): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDirection = 'desc';
    }
  }
}
