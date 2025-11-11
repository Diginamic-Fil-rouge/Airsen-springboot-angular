import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Sidebar State Service
 *
 * Manages the global sidebar expansion state across the application.
 * This service allows the app component to react to sidebar state changes
 * and adjust layout margins accordingly.
 */
@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  /** Internal state for sidebar expansion */
  private sidebarExpandedSubject = new BehaviorSubject<boolean>(true);

  /** Observable for components to subscribe to sidebar state changes */
  public sidebarExpanded$: Observable<boolean> = this.sidebarExpandedSubject.asObservable();

  constructor() {
    // Initialize from localStorage if available
    const savedState = localStorage.getItem('sidebarExpanded');
    if (savedState !== null) {
      this.sidebarExpandedSubject.next(JSON.parse(savedState));
    }
  }

  /**
   * Get current sidebar expansion state
   * @returns true if sidebar is expanded, false if collapsed
   */
  getSidebarState(): boolean {
    return this.sidebarExpandedSubject.value;
  }

  /**
   * Set sidebar expansion state
   * @param isExpanded - true to expand sidebar, false to collapse
   */
  setSidebarState(isExpanded: boolean): void {
    this.sidebarExpandedSubject.next(isExpanded);
    localStorage.setItem('sidebarExpanded', JSON.stringify(isExpanded));
  }

  /**
   * Toggle sidebar expansion state
   * @returns new state after toggle
   */
  toggleSidebar(): boolean {
    const newState = !this.sidebarExpandedSubject.value;
    this.setSidebarState(newState);
    return newState;
  }
}
