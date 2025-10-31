import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@/shared/shared.module';

import { HomeRoutingModule } from './home-routing.module';
import { HomeComponent } from './home.component';

/**
 * HomeModule - Lazy-loaded home/landing page feature module
 *
 * This module encapsulates the home page, which serves as the landing page
 * for visitors and provides an overview of the AIRSEN platform.
 * It is lazy-loaded to optimize initial load performance.
 *
 * Components:
 * - HomeComponent: Landing page with platform overview and featured content
 *
 * Features:
 * - Platform introduction and value proposition
 * - Featured air quality data for major French cities
 * - Call-to-action for user registration/login
 * - Quick navigation to map and forum features
 * - Environmental awareness and educational content
 * - Recent alerts preview
 *
 * Dependencies:
 * - SharedModule: Material components, forms, shared utilities
 *
 * Routes:
 * - '' (root) → HomeComponent
 *
 * Note: This module handles the default route ('/') for the application.
 */
@NgModule({
  declarations: [
    HomeComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeRoutingModule
  ]
})
export class HomeModule { }
