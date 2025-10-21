import { Component, OnInit, OnDestroy, inject, AfterViewInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '@/auth/services/auth.service';
import { AuthUser } from '@/auth/models/auth.model';
import * as L from 'leaflet';
/**
 * DashboardComponent - Main landing page after authentication
 *
 * Features:
 * - Display authenticated user information
 * - Access to environmental data features
 * - Quick navigation to main app sections
 * - User profile management
 * - Logout functionality
 */
@Component({
  standalone: false,
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit, OnDestroy, AfterViewInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  currentUser: AuthUser | null = null;
  isLoading = true;
  private destroy$ = new Subject<void>();
  private map: any;


  ngOnInit(): void {
    this.loadUserData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    if (!this.currentUser){
      this.router.navigate(['/auth/login']);
    }
    this.initMap();
  }

  private initMap(): void {
    this.map = L.map('map', {
      // coordonnées du milieu de la france
      center: [ 46.3622, 1.5231 ],
      zoom: 6
    });

    const tiles = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      minZoom: 3,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    });

    tiles.addTo(this.map);
  }

  /**
   * Load current user data from AuthService
   */
  private loadUserData(): void {
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.currentUser = user;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading user data:', error);
          this.isLoading = false;
        }
      });
  }

  /**
   * Navigate to specified route
   */
  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  /**
   * Handle user logout
   */
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
