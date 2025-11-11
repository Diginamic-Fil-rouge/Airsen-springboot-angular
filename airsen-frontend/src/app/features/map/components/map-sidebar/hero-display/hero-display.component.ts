import { ChangeDetectionStrategy, Component, Input } from "@angular/core";

@Component({
  standalone: false,
  selector: "app-hero-display",
  templateUrl: "./hero-display.component.html",
  styleUrls: ["./hero-display.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeroDisplayComponent {
  @Input() atmoIndex: number | null = null;
  @Input() qualifier: string | null = null;
  @Input() color: string | null = null;

  get displayQualifier(): string {
    return this.qualifier ?? "Indice indisponible";
  }
}
