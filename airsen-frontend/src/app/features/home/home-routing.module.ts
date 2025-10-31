import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home.component';

/**
 * HomeRoutingModule - Routing configuration for home feature
 *
 * This module defines routes for the home/landing page:
 * - '' (root) → HomeComponent
 *
 * The home page displays:
 * - Welcome message and platform overview
 * - Featured air quality information for major cities
 * - Call-to-action to register/login
 * - Quick links to map and forum
 * - Environmental awareness content
 *
 * Lazy loading configuration in AppRoutingModule:
 * {
 *   path: '',
 *   loadChildren: () => import('./features/home/home.module').then(m => m.HomeModule)
 * }
 *
 * Note: This is the default route, so the path is empty string.
 */
const routes: Routes = [
  {
    path: '',
    component: HomeComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class HomeRoutingModule { }
