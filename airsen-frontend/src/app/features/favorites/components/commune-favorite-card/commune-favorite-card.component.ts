import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { UserFavoriteResponse } from '@/shared/models/favorite.model';

@Component({
  standalone: false,
  selector: 'app-commune-favorite-card',
  templateUrl: './commune-favorite-card.component.html',
  styleUrls: ['./commune-favorite-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CommuneFavoriteCardComponent {
  @Input() favorite!: UserFavoriteResponse;
  @Input() isPrimary = false;
  @Output() viewDetails = new EventEmitter<string>();
  @Output() removeFavorite = new EventEmitter<string>();
  @Output() setPrimary = new EventEmitter<string>();

  onViewDetails(): void {
    this.viewDetails.emit(this.favorite.communeInseeCode);
  }

  onRemoveFavorite(): void {
    this.removeFavorite.emit(this.favorite.communeInseeCode);
  }

  onSetPrimary(): void {
    this.setPrimary.emit(this.favorite.communeInseeCode);
  }
}