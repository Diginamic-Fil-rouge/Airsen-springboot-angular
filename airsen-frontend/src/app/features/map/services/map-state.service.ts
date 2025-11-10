import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * MapStateService
 *
 * Purpose: Centralized RxJS state for map UI (selected commune, panel state,
 * map style, heatmap toggle). Provides a single source of truth for map feature.
 */
@Injectable({ providedIn: 'root' })
export class MapStateService {
  readonly isPanelOpen$ = new BehaviorSubject<boolean>(true);
}

