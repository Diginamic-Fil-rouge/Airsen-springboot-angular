import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from "@angular/core";
import { Commune, CommuneWithAirQuality } from "@/shared/models/commune.model";
import { MapSidebarDisplayMode } from "./map-sidebar.types";

/**
 * MapSidebarComponent
 *
 * Container for the AQI detail sidebar that sits next to the Leaflet map.
 * Handles responsive display (fixed, overlay, full screen) and exposes
 * hooks for showing/hiding on smaller breakpoints.
 */
@Component({
  standalone: false,
  selector: "app-map-sidebar",
  templateUrl: "./map-sidebar.component.html",
  styleUrls: ["./map-sidebar.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapSidebarComponent {
  @Input() commune: CommuneWithAirQuality | null = null;
  @Input() open = true;
  @Input() displayMode: MapSidebarDisplayMode = "desktop";
  @Output() openChange = new EventEmitter<boolean>();
  @Output() clearSelection = new EventEmitter<void>();

  get isDesktop(): boolean {
    return this.displayMode === "desktop";
  }

  get isTablet(): boolean {
    return this.displayMode === "tablet";
  }

  get isMobile(): boolean {
    return this.displayMode === "mobile";
  }

  /**
   * Collapses the sidebar on non-desktop layouts.
   */
  handleClose(): void {
    if (this.isDesktop) {
      return;
    }

    this.openChange.emit(false);
  }

  /**
   * Clears the current commune selection and hides the sidebar on mobile/tablet.
   */
  handleClearSelection(): void {
    this.clearSelection.emit();

    if (!this.isDesktop) {
      this.openChange.emit(false);
    }
  }
}
