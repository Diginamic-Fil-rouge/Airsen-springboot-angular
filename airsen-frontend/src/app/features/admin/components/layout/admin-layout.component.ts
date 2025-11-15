import { Component } from '@angular/core';

/**
 * AdminLayoutComponent
 *
 * Provides the layout structure for the admin dashboard with:
 * - Sidebar navigation menu
 * - Header with user info
 * - Content area for router-outlet
 */
@Component({
  selector: 'app-admin-layout',
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss'],
  standalone: false
})
export class AdminLayoutComponent {
  sidebarOpen = true;

  constructor() {}

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }
}
