import { ChangeDetectionStrategy, Component } from "@angular/core";

interface LegendEntry {
  label: string;
  color: string;
}

@Component({
  standalone: false,
  selector: "app-map-legend",
  templateUrl: "./map-legend.component.html",
  styleUrls: ["./map-legend.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapLegendComponent {
  readonly legend: LegendEntry[] = [
    { label: "Bon (0-50)", color: "#4CAF50" },
    { label: "Moyen (51-100)", color: "#FFC107" },
    { label: "Dégradé (101-150)", color: "#FF9800" },
    { label: "Mauvais (151-200)", color: "#F44336" },
    { label: "Très mauvais (200+)", color: "#9C27B0" },
  ];
}
