import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '@/auth/services/auth.service';
import { AuthUser } from '@/auth/models/auth.model';

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
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);

  currentUser: AuthUser | null = null;
  isLoading = true;
  private destroy$ = new Subject<void>();

  // Navigation cards for dashboard
  navigationCards = [
    {
      title: 'Air Quality',
      icon: 'air',
      description: 'View real-time air quality data',
      route: '/air-quality',
      color: 'primary'
    },
    {
      title: 'Weather',
      icon: 'wb_sunny',
      description: 'Check weather conditions',
      route: '/weather',
      color: 'accent'
    },
    {
      title: 'Map',
      icon: 'map',
      description: 'Explore environmental data on map',
      route: '/map',
      color: 'primary'
    },
    {
      title: 'Forum',
      icon: 'forum',
      description: 'Join community discussions',
      route: '/forum',
      color: 'accent'
    },
    {
      title: 'Alerts',
      icon: 'notifications',
      description: 'Manage your alert preferences',
      route: '/alerts',
      color: 'warn'
    },
    {
      title: 'Profile',
      icon: 'person',
      description: 'Update your profile settings',
      route: '/profile',
      color: 'primary'
    }
  ];

  ngOnInit(): void {
    this.loadUserData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

  /**
   * Get user initials for avatar
   */
  getUserInitials(): string {
    if (!this.currentUser) {
      return '?';
    }

    const firstInitial = this.currentUser.firstName?.charAt(0).toUpperCase() || '';
    const lastInitial = this.currentUser.lastName?.charAt(0).toUpperCase() || '';

    return `${firstInitial}${lastInitial}`;
  }

  /**
   * Get greeting based on time of day
   */
  getGreeting(): string {
    const hour = new Date().getHours();

    if (hour < 12) {
      return 'Good Morning';
    } else if (hour < 18) {
      return 'Good Afternoon';
    } else {
      return 'Good Evening';
    }
  }

  /**
   * Get user's full name
   */
  getUserFullName(): string {
    if (!this.currentUser) {
      return 'User';
    }

    return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
  }

  /**
   * Get role display name
   */
  getRoleDisplay(): string {
    if (!this.currentUser) {
      return '';
    }

    return this.currentUser.role.charAt(0) + this.currentUser.role.slice(1).toLowerCase();
  }
}
