import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  standalone: false,
  selector: 'app-favorites-loading-skeleton',
  templateUrl: './favorites-loading-skeleton.component.html',
  styleUrls: ['./favorites-loading-skeleton.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FavoritesLoadingSkeletonComponent {
  skeletonCards = [1, 2, 3]; // Show 3 skeleton cards while loading
}