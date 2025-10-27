import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AuthService } from '@/auth/services/auth.service';
import { AuthUser } from '@/auth/models/auth.model';

type QuickActionKey = 'map' | 'alerts' | 'forum' | 'favorites' | 'export';

interface QuickActionCard {
  title: string;
  subtitle: string;
  icon: string;
  action: QuickActionKey;
  badge?: string;
}

interface AlertSummaryItem {
  id: number;
  title: string;
  location: string;
  severity: 'low' | 'medium' | 'high';
  status: 'active' | 'unread' | 'resolved';
  icon: string;
  timestamp: string;
}

interface UserStatsSnapshot {
  favoriteIndicators: number;
  alertsReceived: number;
  lastExport: string;
  forumPosts: number;
  profileCompletion: number;
}

@Component({
  standalone: false,
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: AuthUser | null = null;
  isLoading = true;
  currentDateTime = new Date();

  quickActions: QuickActionCard[] = [];
  recentAlerts: AlertSummaryItem[] = [
    {
      id: 1,
      title: 'PM2.5 levels rising',
      location: 'Lyon · 5e arrondissement',
      severity: 'high',
      status: 'active',
      icon: 'warning',
      timestamp: '15 minutes ago'
    },
    {
      id: 2,
      title: 'Ozone threshold exceeded',
      location: 'Paris · 15e arrondissement',
      severity: 'medium',
      status: 'unread',
      icon: 'error_outline',
      timestamp: '45 minutes ago'
    },
    {
      id: 3,
      title: 'Pollution alert resolved',
      location: 'Marseille · Vieux-Port',
      severity: 'low',
      status: 'resolved',
      icon: 'check_circle',
      timestamp: '2 hours ago'
    }
  ];

  userStats: UserStatsSnapshot = {
    favoriteIndicators: 5,
    alertsReceived: 18,
    lastExport: '12 March 2024',
    forumPosts: 9,
    profileCompletion: 82
  };

  forumUnreadCount = 2;

  private destroy$ = new Subject<void>();
  private clockIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.subscribeToUser();
    this.startClock();
    this.buildQuickActions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    if (this.clockIntervalId) {
      clearInterval(this.clockIntervalId);
    }
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => this.router.navigate(['/auth/login'])
    });
  }

  handleQuickAction(action: QuickActionKey): void {
    switch (action) {
      case 'map':
        this.router.navigate(['/map']);
        break;
      case 'alerts':
        this.router.navigate(['/map'], {
          queryParams: { view: 'alerts' }
        });
        break;
      case 'forum':
        this.router.navigate(['/forum']);
        break;
      case 'favorites':
        this.router.navigate(['/profile'], {
          queryParams: { tab: 'favorites' }
        });
        break;
      case 'export':
        this.router.navigate(['/profile'], {
          queryParams: { tab: 'exports' }
        });
        break;
      default:
        break;
    }
  }

  filterAlertsByFavorites(): void {
    this.router.navigate(['/map'], {
      queryParams: { view: 'alerts', scope: 'favorites' }
    });
  }

  goToHistoricData(): void {
    this.router.navigate(['/map'], {
      queryParams: { view: 'history' }
    });
  }

  get welcomeName(): string {
    return this.currentUser?.firstName || 'there';
  }

  get avatarInitials(): string {
    if (!this.currentUser) {
      return 'AA';
    }

    const first = this.currentUser.firstName?.trim().charAt(0).toUpperCase() ?? '';
    const last = this.currentUser.lastName?.trim().charAt(0).toUpperCase() ?? '';
    let initials = `${first}${last}`.replace(/\s+/g, '');

    if (!initials) {
      return 'AA';
    }

    if (initials.length === 1) {
      return `${initials}${initials}`;
    }

    return initials.slice(0, 2);
  }

  private subscribeToUser(): void {
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

  private startClock(): void {
    this.currentDateTime = new Date();
    this.clockIntervalId = setInterval(() => {
      this.currentDateTime = new Date();
    }, 60000);
  }

  private buildQuickActions(): void {
    const actionableAlerts = this.recentAlerts.filter(alert => alert.status === 'active' || alert.status === 'unread').length;

    this.quickActions = [
      {
        title: 'View Map',
        subtitle: 'Jump to live air quality layers.',
        icon: 'map',
        action: 'map'
      },
      {
        title: 'My Notifications & Alerts',
        subtitle: `${actionableAlerts} new alerts waiting`,
        icon: 'notifications_active',
        action: 'alerts',
        badge: actionableAlerts ? `${actionableAlerts}` : undefined
      },
      {
        title: 'Go to Forum',
        subtitle: `${this.forumUnreadCount} threads need your reply`,
        icon: 'forum',
        action: 'forum',
        badge: this.forumUnreadCount ? `${this.forumUnreadCount}` : undefined
      },
      {
        title: 'My Favorites',
        subtitle: `${this.userStats.favoriteIndicators} indicators saved`,
        icon: 'favorite',
        action: 'favorites',
        badge: this.userStats.favoriteIndicators ? `${this.userStats.favoriteIndicators}` : undefined
      },
      {
        title: 'Export History',
        subtitle: `Last export: ${this.userStats.lastExport}`,
        icon: 'download',
        action: 'export'
      }
    ];
  }
}
