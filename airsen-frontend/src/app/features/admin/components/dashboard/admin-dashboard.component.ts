import { Component, OnInit } from '@angular/core';
import { AdminService, AdminStatistics } from '../../services/admin.service';

/**
 * AdminDashboardComponent
 *
 * Displays admin overview statistics including:
 * - User counts (total, active, suspended, new this week)
 * - Alert counts (total, active)
 * - Campaign counts (total, in progress)
 * - Forum statistics
 * - Notification statistics
 */
@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
  standalone: false
})
export class AdminDashboardComponent implements OnInit {
  statistics: AdminStatistics | null = null;
  loading = true;
  error: string | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadStatistics();
  }

  loadStatistics(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getStatistics().subscribe({
      next: (stats) => {
        this.statistics = stats;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load statistics. Please try again.';
        this.loading = false;
        console.error('Error loading statistics:', err);
      }
    });
  }

  refresh(): void {
    this.loadStatistics();
  }
}
