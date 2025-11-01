import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';

import { AuthService } from '@/auth/services/auth.service';
import { AuthUser } from '@/auth/models/auth.model';
import { AlertService } from '@/core/services/alert.service';
import { FavoritesService } from '@/features/favorites/services/favorites.service';
import { CampaignNotification, NotificationDeliveryStatus } from '@/shared/models';
import { Favorite } from '@/shared/models';
import { QuickActionCard, QuickActionKey } from './models/quick-action';
import { AlertSummaryItem } from './models/alert-summary';
import { UserStatsSnapshot } from './models/user-stats';
import { StatClickEvent } from './components/stats-panel/stats-panel';

@Component({
  standalone: false,
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: AuthUser | null = null;
  isLoading = true;
  error: string | null = null;
  currentDateTime = new Date();

  quickActions: QuickActionCard[] = [];
  recentAlerts: AlertSummaryItem[] = [];

  userStats: UserStatsSnapshot = {
    favoriteIndicators: 0,
    alertsReceived: 0,
    lastExport: 'N/A',
    forumPosts: 0,
    profileCompletion: 0
  };

  forumUnreadCount = 0;

  private destroy$ = new Subject<void>();
  private clockIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private authService: AuthService,
    private alertService: AlertService,
    private favoritesService: FavoritesService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.subscribeToUser();
    this.startClock();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    if (this.clockIntervalId) {
      clearInterval(this.clockIntervalId);
    }
  }

  // Event handlers for child components
  onQuickAction(action: QuickActionKey): void {
    this.handleQuickAction(action);
  }

  onAlertClick(alertId: number): void {
    this.router.navigate(['/map'], {
      queryParams: { view: 'alerts', alertId }
    });
  }

  onViewAllAlerts(): void {
    this.router.navigate(['/map'], {
      queryParams: { view: 'alerts' }
    });
  }

  onFilterAlertsByFavorites(): void {
    this.router.navigate(['/map'], {
      queryParams: { view: 'alerts', scope: 'favorites' }
    });
  }

  onStatClick(event: StatClickEvent): void {
    this.handleStatClick(event);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => this.router.navigate(['/auth/login'])
    });
  }

  goToHistoricData(): void {
    this.router.navigate(['/map'], {
      queryParams: { view: 'history' }
    });
  }

  // Getters for template
  get welcomeName(): string {
    return this.currentUser?.firstName || 'là';
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

  retryLoad(): void {
    if (this.currentUser) {
      this.loadDashboardData(this.currentUser.id);
    }
  }

  // Private methods
  private handleQuickAction(action: QuickActionKey): void {
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

  private handleStatClick(event: StatClickEvent): void {
    switch (event.statKey) {
      case 'favoriteIndicators':
        this.router.navigate(['/profile'], {
          queryParams: { tab: 'favorites' }
        });
        break;
      case 'alertsReceived':
        this.router.navigate(['/map'], {
          queryParams: { view: 'alerts' }
        });
        break;
      case 'forumPosts':
        this.router.navigate(['/forum']);
        break;
      default:
        break;
    }
  }

  private subscribeToUser(): void {
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.currentUser = user;
          if (user) {
            this.loadDashboardData(user.id);
          } else {
            this.isLoading = false;
          }
        },
        error: (error) => {
          console.error('Error loading user data:', error);
          this.error = 'Échec du chargement des données utilisateur';
          this.isLoading = false;
        }
      });
  }

  /**
   * Loads dashboard data: recent notifications and user favorites.
   * Uses forkJoin to fetch data in parallel for optimal performance.
   */
  private loadDashboardData(userId: number): void {
    this.isLoading = true;
    this.error = null;

    forkJoin({
      notifications: this.alertService.getRecentNotifications(userId, 3).pipe(
        catchError(err => {
          console.error('Error loading notifications:', err);
          return of([]);
        })
      ),
      favorites: this.favoritesService.getUserFavorites(userId).pipe(
        catchError(err => {
          console.error('Error loading favorites:', err);
          return of([]);
        })
      )
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ notifications, favorites }) => {
          this.processNotifications(notifications);
          this.processFavorites(favorites);
          this.buildQuickActions();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading dashboard data:', error);
          this.error = 'Échec du chargement des données du tableau de bord';
          this.isLoading = false;
        }
      });
  }

  /**
   * Converts CampaignNotification to AlertSummaryItem for UI display.
   */
  private processNotifications(notifications: CampaignNotification[]): void {
    this.recentAlerts = notifications.map(notification => ({
      id: notification.id,
      title: this.extractTitleFromNotification(notification),
      location: this.extractLocationFromNotification(notification),
      severity: this.mapStatusToSeverity(notification.status),
      status: this.mapDeliveryStatus(notification.status),
      icon: this.getIconForStatus(notification.status),
      timestamp: this.formatTimestamp(notification.createdAt)
    }));

    // Update stats: count total notifications (SENT + PENDING + FAILED)
    this.userStats.alertsReceived = notifications.length;
  }

  /**
   * Processes user favorites and updates stats.
   */
  private processFavorites(favorites: Favorite[]): void {
    this.userStats.favoriteIndicators = favorites.length;
  }

  /**
   * Extracts title from campaign notification.
   * Backend may store campaign title or message content.
   */
  private extractTitleFromNotification(notification: CampaignNotification): string {
    // Assuming campaignTitle exists in backend response (check actual API response)
    return `Alerte Environnementale #${notification.campaignId}`;
  }

  /**
   * Extracts location from notification (if available in backend).
   * For now, returns generic message. Update when backend provides scope details.
   */
  private extractLocationFromNotification(notification: CampaignNotification): string {
    return notification.recipientEmail; // Placeholder - backend may add scopeName field
  }

  /**
   * Maps NotificationDeliveryStatus to severity for UI styling.
   */
  private mapStatusToSeverity(status: NotificationDeliveryStatus): 'low' | 'medium' | 'high' {
    switch (status) {
      case 'FAILED':
        return 'high';
      case 'PENDING':
        return 'medium';
      case 'SENT':
      default:
        return 'low';
    }
  }

  /**
   * Maps NotificationDeliveryStatus to AlertSummaryItem status.
   */
  private mapDeliveryStatus(status: NotificationDeliveryStatus): 'pending' | 'sent' | 'failed' {
    return status.toLowerCase() as 'pending' | 'sent' | 'failed';
  }

  /**
   * Gets Material icon for notification status.
   */
  private getIconForStatus(status: NotificationDeliveryStatus): string {
    switch (status) {
      case 'FAILED':
        return 'error';
      case 'PENDING':
        return 'notifications_active';
      case 'SENT':
      default:
        return 'check_circle';
    }
  }

  /**
   * Formats timestamp to relative time (e.g., "15 minutes ago").
   */
  private formatTimestamp(date: Date): string {
    const now = new Date();
    const createdAt = new Date(date);
    const diffMs = now.getTime() - createdAt.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} minute${diffMins > 1 ? 's' : ''}`;
    if (diffHours < 24) return `Il y a ${diffHours} heure${diffHours > 1 ? 's' : ''}`;
    return `Il y a ${diffDays} jour${diffDays > 1 ? 's' : ''}`;
  }

  private startClock(): void {
    this.currentDateTime = new Date();
    this.clockIntervalId = setInterval(() => {
      this.currentDateTime = new Date();
    }, 60000);
  }

  private buildQuickActions(): void {
    // Count pending + failed notifications as "actionable"
    const actionableAlerts = this.recentAlerts.filter(
      alert => alert.status === 'pending' || alert.status === 'failed'
    ).length;

    this.quickActions = [
      {
        title: 'Voir la Carte',
        subtitle: 'Accéder aux couches de qualité de l\'air en temps réel.',
        icon: 'map',
        action: 'map'
      },
      {
        title: 'Mes Notifications & Alertes',
        subtitle: actionableAlerts > 0
          ? `${actionableAlerts} nouvelle${actionableAlerts > 1 ? 's' : ''} alerte${actionableAlerts > 1 ? 's' : ''} en attente`
          : 'Aucune nouvelle alerte',
        icon: 'notifications_active',
        action: 'alerts',
        badge: actionableAlerts ? `${actionableAlerts}` : undefined
      },
      {
        title: 'Aller au Forum',
        subtitle: this.forumUnreadCount > 0
          ? `${this.forumUnreadCount} discussion${this.forumUnreadCount > 1 ? 's' : ''} nécessite${this.forumUnreadCount > 1 ? 'nt' : ''} votre réponse`
          : 'Aucune discussion non lue',
        icon: 'forum',
        action: 'forum',
        badge: this.forumUnreadCount ? `${this.forumUnreadCount}` : undefined
      },
      {
        title: 'Mes Favoris',
        subtitle: `${this.userStats.favoriteIndicators} indicateur${this.userStats.favoriteIndicators !== 1 ? 's' : ''} enregistré${this.userStats.favoriteIndicators !== 1 ? 's' : ''}`,
        icon: 'favorite',
        action: 'favorites',
        badge: this.userStats.favoriteIndicators ? `${this.userStats.favoriteIndicators}` : undefined
      },
      {
        title: 'Historique d\'Export',
        subtitle: `Dernier export : ${this.userStats.lastExport}`,
        icon: 'download',
        action: 'export'
      }
    ];
  }
}