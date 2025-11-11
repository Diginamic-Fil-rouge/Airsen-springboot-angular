import { ChangeDetectionStrategy, Component, Input } from "@angular/core";

@Component({
  standalone: false,
  selector: "app-trend-chart",
  templateUrl: "./trend-chart.component.html",
  styleUrls: ["./trend-chart.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrendChartComponent {
  @Input() inseeCode: string | null = null;
  @Input() mode: "aqi" | "weather" = "aqi";
}
