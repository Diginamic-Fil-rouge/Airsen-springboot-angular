import { Component, Input, Output, EventEmitter } from "@angular/core";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";

@Component({
  standalone: false,
  selector: "app-commune-list",
  templateUrl: "./commune-list.component.html",
  styleUrls: ["./commune-list.component.scss"],
})
export class CommuneListComponent {
  @Input() communes: CommuneWithAirQuality[] = [];
  @Input() selectedCommune: CommuneWithAirQuality | null = null;
  @Output() communeSelected = new EventEmitter<CommuneWithAirQuality>();

  searchQuery: string = "";
  sortBy: "name" | "aqi" | "population" = "aqi";
  sortDirection: "asc" | "desc" = "desc";

  getAqiColor(commune: CommuneWithAirQuality): string {
    return commune.currentAirQuality?.color || "#999999";
  }

  getAqiLabel(commune: CommuneWithAirQuality): string {
    return commune.currentAirQuality?.qualifier || "Inconnu";
  }

  getAtmoIndex(commune: CommuneWithAirQuality): number {
    return commune.currentAirQuality?.atmoIndex || 0;
  }

  get filteredAndSortedCommunes(): CommuneWithAirQuality[] {
    let result = [...this.communes];

    // Filter by search query
    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      result = result.filter((c) => c.name.toLowerCase().includes(query) || c.inseeCode.includes(query));
    }

    // Sort
    result.sort((a, b) => {
      let compareValue = 0;

      switch (this.sortBy) {
        case "name":
          compareValue = a.name.localeCompare(b.name);
          break;
        case "aqi":
          const aAtmoIndex = a.currentAirQuality?.atmoIndex || 0;
          const bAtmoIndex = b.currentAirQuality?.atmoIndex || 0;
          compareValue = aAtmoIndex - bAtmoIndex;
          break;
        case "population":
          compareValue = (a.population || 0) - (b.population || 0);
          break;
      }

      return this.sortDirection === "asc" ? compareValue : -compareValue;
    });

    return result;
  }

  onCommuneClick(commune: CommuneWithAirQuality): void {
    this.communeSelected.emit(commune);
  }

  toggleSort(field: "name" | "aqi" | "population"): void {
    if (this.sortBy === field) {
      this.sortDirection = this.sortDirection === "asc" ? "desc" : "asc";
    } else {
      this.sortBy = field;
      this.sortDirection = "desc";
    }
  }
}
