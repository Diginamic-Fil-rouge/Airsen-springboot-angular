import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { AlertSummaryItem } from '../../models/alert-summary';

@Component({
  selector: 'app-alert-summary',
  standalone: false,
  templateUrl: './alert-summary.html',
  styleUrls: ['./alert-summary.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AlertSummaryComponent {
  @Input() alerts: AlertSummaryItem[] = [];
  @Input() showPagination = false;
  @Input() itemsPerPage = 5;
  @Input() showFavoritesFilter = false;
  @Output() alertClick = new EventEmitter<number>();
  @Output() viewAllAlerts = new EventEmitter<void>();
  @Output() filterByFavorites = new EventEmitter<void>();

  currentPage = 1;

  get paginatedAlerts(): AlertSummaryItem[] {
    if (!this.showPagination) {
      return this.alerts;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.alerts.slice(startIndex, endIndex);
  }

  get totalPages(): number {
    return Math.ceil(this.alerts.length / this.itemsPerPage);
  }

  get hasPreviousPage(): boolean {
    return this.currentPage > 1;
  }

  get hasNextPage(): boolean {
    return this.currentPage < this.totalPages;
  }

  onAlertClick(alertId: number): void {
    this.alertClick.emit(alertId);
  }

  onViewAllAlerts(): void {
    this.viewAllAlerts.emit();
  }

  onFilterByFavorites(): void {
    this.filterByFavorites.emit();
  }

  previousPage(): void {
    if (this.hasPreviousPage) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.hasNextPage) {
      this.currentPage++;
    }
  }
}