import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  private sidebarExpandedSubject = new BehaviorSubject<boolean>(
    this.getSidebarStateFromStorage()
  );
  public sidebarExpanded$: Observable<boolean> = this.sidebarExpandedSubject.asObservable();

  constructor() {}

  /**
   * Get the current sidebar expanded state
   */
  getSidebarState(): boolean {
    return this.sidebarExpandedSubject.value;
  }

  /**
   * Set the sidebar state and persist to localStorage
   */
  setSidebarState(isExpanded: boolean): void {
    this.sidebarExpandedSubject.next(isExpanded);
    localStorage.setItem('sidebarExpanded', JSON.stringify(isExpanded));
  }

  /**
   * Toggle the sidebar state
   */
  toggleSidebar(): boolean {
    const newState = !this.sidebarExpandedSubject.value;
    this.setSidebarState(newState);
    return newState;
  }

  /**
   * Get the sidebar state from localStorage
   */
  private getSidebarStateFromStorage(): boolean {
    const stored = localStorage.getItem('sidebarExpanded');
    return stored ? JSON.parse(stored) : true; // Default to expanded
  }
}
