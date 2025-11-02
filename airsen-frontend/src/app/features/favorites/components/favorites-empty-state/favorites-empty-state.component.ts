import { Component, ChangeDetectionStrategy, Output, EventEmitter } from '@angular/core';

@Component({
  standalone: false,
  selector: 'app-favorites-empty-state',
  templateUrl: './favorites-empty-state.component.html',
  styleUrls: ['./favorites-empty-state.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FavoritesEmptyStateComponent {
  @Output() browseCommunes = new EventEmitter<void>();

  onBrowseCommunes(): void {
    this.browseCommunes.emit();
  }
}