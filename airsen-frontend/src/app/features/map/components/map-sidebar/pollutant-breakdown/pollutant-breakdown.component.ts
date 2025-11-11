import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";

type PollutantKey = "pm25" | "pm10" | "o3" | "no2" | "so2";

const POLLUTANT_KEYS: PollutantKey[] = ["pm25", "pm10", "o3", "no2", "so2"];

const POLLUTANT_LABELS: Record<PollutantKey, string> = {
  pm25: "PM2.5",
  pm10: "PM10",
  o3: "O₃",
  no2: "NO₂",
  so2: "SO₂",
};

@Component({
  standalone: false,
  selector: "app-pollutant-breakdown",
  templateUrl: "./pollutant-breakdown.component.html",
  styleUrls: ["./pollutant-breakdown.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PollutantBreakdownComponent {
  @Input() pollutants: CommuneWithAirQuality["pollutants"] | null = null;

  get pollutantList(): Array<{ key: PollutantKey; label: string; value: number | null }> {
    return POLLUTANT_KEYS.map((key) => ({
      key,
      label: POLLUTANT_LABELS[key],
      value: this.pollutants?.[key] ?? null,
    }));
  }

  getProgressValue(value: number | null): number {
    if (value == null) {
      return 0;
    }

    return Math.max(0, Math.min(value, 200));
  }
}
