import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";

@Component({
  standalone: false,
  selector: "app-location-header",
  templateUrl: "./location-header.component.html",
  styleUrls: ["./location-header.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationHeaderComponent {
  @Input() commune: CommuneWithAirQuality | null = null;
}
