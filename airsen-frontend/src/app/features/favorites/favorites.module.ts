import { NgModule } from '@angular/core';

import { FavoritesRoutingModule } from './favorites-routing.module';
import { FavoritesComponent } from './pages/favorites/favorites.component';
import { CommuneFavoriteCardComponent } from './components/commune-favorite-card/commune-favorite-card.component';
import { FavoritesEmptyStateComponent } from './components/favorites-empty-state/favorites-empty-state.component';
import { FavoritesLoadingSkeletonComponent } from './components/favorites-loading-skeleton/favorites-loading-skeleton.component';

import { SharedModule } from '@/shared/shared.module';

@NgModule({
  declarations: [
    FavoritesComponent,
    CommuneFavoriteCardComponent,
    FavoritesEmptyStateComponent,
    FavoritesLoadingSkeletonComponent
  ],
  imports: [
    SharedModule,
    FavoritesRoutingModule
  ]
})
export class FavoritesModule { }