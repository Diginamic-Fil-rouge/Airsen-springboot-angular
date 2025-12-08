import { Component, OnDestroy, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { Subject, forkJoin, of } from "rxjs";
import { takeUntil, catchError, map } from "rxjs/operators";

import { AuthService } from "@/auth/services/auth.service";
import { AuthUser } from "@/auth/models/auth.model";
import { AlertService } from "@/core/services/alert.service";
import { FavoriteService } from "@/features/favorites/services/favorite.service";
import { AirQualityService, AirQualityResponse } from "@/features/map/services/air-quality.service";
import { WeatherService } from "@/core/services/weather.service";
import { ThreadService } from "@/features/forum/services/thread.service";
import { UserProfileService } from "@/features/profile/services/user-profile.service";
import { ExportService } from "@/core/services/export.service";
import { Thread } from "@/features/forum/models/thread.model";
import { WeatherData } from "@/shared/models/weather.model";
import { CampaignNotification, NotificationDeliveryStatus } from "@/shared/models";
import { UserFavoriteResponse } from "@/shared/models/favorite.model";
import { QuickActionCard, QuickActionKey } from "./models/quick-action";
import { AlertSummaryItem } from "./models/alert-summary";
import { UserStatsSnapshot } from "./models/user-stats";
import { StatClickEvent } from "./components/stats-panel/stats-panel";
import {
  fadeInAnimation,
  slideUpAnimation,
  listStaggerAnimation,
  dashboardWidgetAnimation,
} from "@/shared/animations/animations";

@Component({
  standalone: false,
  selector: "app-dashboard",
  templateUrl: "./dashboard.component.html",
  styleUrls: ["./dashboard.component.scss"],
  animations: [fadeInAnimation, slideUpAnimation, listStaggerAnimation, dashboardWidgetAnimation],
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: AuthUser | null = null;
  isLoading = true;
  error: string | null = null;
  currentDateTime = new Date();

  quickActions: QuickActionCard[] = [];
  recentAlerts: AlertSummaryItem[] = [];

  // User stats - will be populated from backend API
  userStats: UserStatsSnapshot = {
    favoriteIndicators: 0,
    alertsReceived: 0,
    lastExport: "N/A",
    forumPosts: 0,
    profileCompletion: 0,
  };

  forumUnreadCount = 0;

  // New properties for Hero Section
  primaryFavoriteAqi: AirQualityResponse | null = null;
  primaryFavoriteWeather: WeatherData | null = null;
  trendingThread: Thread | null = null;

  private destroy$ = new Subject<void>();
  private clockIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private authService: AuthService,
    private alertService: AlertService,
    private favoriteService: FavoriteService,
    private airQualityService: AirQualityService,
    private weatherService: WeatherService,
    private threadService: ThreadService,
    private userProfileService: UserProfileService,
    private exportService: ExportService,
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

  onAlertClick(_alertId: number): void {
    // TODO: Implement map feature for alert view
    console.warn("Map feature not yet implemented");
  }

  onViewAllAlerts(): void {
    // TODO: Implement map feature for alerts view
    console.warn("Map feature not yet implemented");
  }

  onFilterAlertsByFavorites(): void {
    // TODO: Implement map feature for alerts on favorites
    console.warn("Map feature not yet implemented");
  }

  onStatClick(event: StatClickEvent): void {
    this.handleStatClick(event);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(["/auth/login"]),
      error: () => this.router.navigate(["/auth/login"]),
    });
  }

  // Getters for template
  get welcomeName(): string {
    return this.currentUser?.firstName || "là";
  }

  get avatarInitials(): string {
    if (!this.currentUser) {
      return "AA";
    }

    const first = this.currentUser.firstName?.trim().charAt(0).toUpperCase() ?? "";
    const last = this.currentUser.lastName?.trim().charAt(0).toUpperCase() ?? "";
    const initials = `${first}${last}`.replace(/\s+/g, "");

    if (!initials) {
      return "AA";
    }

    if (initials.length === 1) {
      return `${initials}${initials}`;
    }

    return initials.slice(0, 2);
  }

  get isAdmin(): boolean {
    return this.currentUser?.role === "ADMIN";
  }

  retryLoad(): void {
    if (this.currentUser) {
      this.loadDashboardData(this.currentUser.id);
    }
  }

  // Private methods
  private handleQuickAction(action: QuickActionKey): void {
    switch (action) {
      case "map":
        this.router.navigate(["/map"]);
        break;
      case "alerts":
        this.router.navigate(["/profile"], {
          queryParams: { tab: "notifications-tab" },
        });
        break;
      case "forum":
        if (this.trendingThread) {
          this.router.navigate(["/forum/thread", this.trendingThread.id]);
        } else {
          this.router.navigate(["/forum"]);
        }
        break;
      case "favorites":
        // For admin: redirect to /profile (Stats Système)
        // For user: redirect to /favorites (Mes Favoris)
        if (this.isAdmin) {
          this.router.navigate(["/profile"]);
        } else {
          this.router.navigate(["/favorites"]);
        }
        break;
      case "export":
        this.router.navigate(["/export"]);
        break;
      default:
        break;
    }
  }

  private handleStatClick(event: StatClickEvent): void {
    switch (event.statKey) {
      case "favoriteIndicators":
        this.router.navigate(["/favorites"]);
        break;
      case "alertsReceived":
        this.router.navigate(["/map"]);
        break;
      case "forumPosts":
        this.router.navigate(["/forum"]);
        break;
      default:
        break;
    }
  }

  private subscribeToUser(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (user) => {
        this.currentUser = user;
        if (user) {
          this.loadDashboardData(user.id);
        } else {
          this.isLoading = false;
        }
      },
      error: (error) => {
        console.error("Error loading user data:", error);
        this.error = "Échec du chargement des données utilisateur";
        this.isLoading = false;
      },
    });
  }

  /**
   * Loads dashboard data: recent notifications, user favorites, and trending thread.
   * Uses forkJoin to fetch data in parallel for optimal performance.
   */
  private loadDashboardData(userId: number): void {
    this.isLoading = true;
    this.error = null;

    forkJoin({
      notifications: this.alertService.getRecentNotifications(userId, 3).pipe(
        catchError((err) => {
          console.error("Error loading notifications:", err);
          return of([]);
        })
      ),
      favorites: this.favoriteService.getUserFavorites(userId).pipe(
        catchError((err) => {
          console.error("Error loading favorites:", err);
          return of([]);
        })
      ),
      trending: this.threadService.getAllThreads().pipe(
        map((page) => {
          // Simple logic to find trending: sort by viewCount descending
          if (page && page.content && page.content.length > 0) {
            return page.content.sort((a, b) => b.viewCount - a.viewCount)[0];
          }
          return null;
        }),
        catchError((err) => {
          console.error("Error loading trending thread:", err);
          return of(null);
        })
      ),
      profile: this.userProfileService.getProfile().pipe(
        catchError((err) => {
          console.error("Error loading user profile:", err);
          return of(null);
        })
      ),
      userThreads: this.threadService.getThreadsByAuthor(userId).pipe(
        catchError((err) => {
          console.error("Error loading user threads:", err);
          return of([]);
        })
      ),
      exportHistory: this.exportService.getExportHistory().pipe(
        catchError((err) => {
          console.error("Error loading export history:", err);
          return of([]);
        })
      ),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ notifications, favorites, trending, profile, userThreads, exportHistory }) => {
          this.processNotifications(notifications);
          this.processFavorites(favorites);
          this.processUserProfile(profile);
          this.processForumPosts(userThreads);
          this.processExportHistory(exportHistory);
          this.trendingThread = trending;

          // Fetch AQI and Weather for primary favorite
          if (favorites.length > 0) {
            const primaryInsee = localStorage.getItem("primaryFavoriteInseeCode");
            const primary = favorites.find((f) => f.communeInseeCode === primaryInsee) || favorites[0];
            this.fetchPrimaryFavoriteData(primary.communeInseeCode);
          }

          this.buildQuickActions();
          this.isLoading = false;
        },
        error: (error) => {
          console.error("Error loading dashboard data:", error);
          this.error = "Échec du chargement des données du tableau de bord";
          this.isLoading = false;
        },
      });
  }

  private fetchPrimaryFavoriteData(inseeCode: string): void {
    forkJoin({
      aqi: this.airQualityService.getAirLatestQuality(inseeCode).pipe(catchError(() => of(null))),
      weather: this.weatherService.getCurrentWeather(inseeCode).pipe(catchError(() => of(null))),
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ aqi, weather }) => {
          this.primaryFavoriteAqi = aqi;
          this.primaryFavoriteWeather = weather;
        },
      });
  }

  /**
   * Converts CampaignNotification to AlertSummaryItem for UI display.
   */
  private processNotifications(notifications: CampaignNotification[]): void {
    this.recentAlerts = notifications.map((notification) => ({
      id: notification.id,
      title: this.extractTitleFromNotification(notification),
      location: this.extractLocationFromNotification(notification),
      severity: this.mapStatusToSeverity(notification.status),
      status: this.mapDeliveryStatus(notification.status),
      icon: this.getIconForStatus(notification.status),
      timestamp: this.formatTimestamp(notification.createdAt),
    }));

    // Update stats: count total notifications (SENT + PENDING + FAILED)
    this.userStats.alertsReceived = notifications.length;
  }

  favorites: UserFavoriteResponse[] = [];

  /**
   * Processes user favorites and updates stats.
   */
  private processFavorites(favorites: UserFavoriteResponse[]): void {
    this.favorites = favorites;
    this.userStats.favoriteIndicators = favorites.length;
  }

  /**
   * Processes user profile and calculates profile completion.
   */
  private processUserProfile(profile: any): void {
    if (profile) {
      this.userStats.profileCompletion = this.userProfileService.calculateProfileCompletion(profile);
    } else {
      this.userStats.profileCompletion = 0;
    }
  }

  /**
   * Counts forum posts (threads) created by the current user.
   */
  private processForumPosts(userThreads: Thread[]): void {
    if (userThreads && Array.isArray(userThreads)) {
      this.userStats.forumPosts = userThreads.length;
    } else {
      this.userStats.forumPosts = 0;
    }
  }

  /**
   * Processes export history and formats last export date.
   */
  private processExportHistory(exportHistory: any[]): void {
    if (exportHistory && exportHistory.length > 0) {
      // Sort by createdAt descending and get the most recent
      const sortedHistory = [...exportHistory].sort((a, b) => {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      });
      const lastExport = sortedHistory[0];

      // Format relative time (e.g., "Il y a 2 jours")
      this.userStats.lastExport = this.formatRelativeTime(lastExport.createdAt);
    } else {
      this.userStats.lastExport = "N/A";
    }
  }

  /**
   * Formats a date as relative time in French.
   */
  private formatRelativeTime(dateString: string | Date): string {
    if (!dateString) {
      return "N/A";
    }

    const date = new Date(dateString);

    // Check if date is valid
    if (isNaN(date.getTime())) {
      console.warn(`Invalid date received: ${dateString}`);
      return "N/A";
    }

    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();

    // Handle future dates or invalid negative differences
    if (diffInMs < 0) {
      return "N/A";
    }

    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInMinutes < 1) {
      return "À l'instant";
    } else if (diffInMinutes < 60) {
      return `Il y a ${diffInMinutes} min`;
    } else if (diffInHours < 24) {
      return `Il y a ${diffInHours}h`;
    } else if (diffInDays === 1) {
      return "Hier";
    } else if (diffInDays < 30) {
      return `Il y a ${diffInDays} jours`;
    } else if (diffInDays < 365) {
      const months = Math.floor(diffInDays / 30);
      return `Il y a ${months} mois`;
    } else {
      const years = Math.floor(diffInDays / 365);
      return `Il y a ${years} an${years > 1 ? 's' : ''}`;
    }
  }

  /**
   * Extracts title from campaign notification.
   * Creates user-friendly alert title based on notification status.
   */
  private extractTitleFromNotification(notification: CampaignNotification): string {
    const statusText = this.getStatusText(notification.status);
    return `Alerte qualité de l'air - ${statusText}`;
  }

  /**
   * Gets French status text for notification.
   */
  private getStatusText(status: NotificationDeliveryStatus): string {
    switch (status) {
      case 'SENT':
        return 'Envoyée';
      case 'PENDING':
        return 'En attente';
      case 'FAILED':
        return 'Échec d\'envoi';
      default:
        return 'Inconnue';
    }
  }

  /**
   * Extracts location/scope from notification.
   * Shows when the alert was created as the location info.
   */
  private extractLocationFromNotification(notification: CampaignNotification): string {
    // Format: "Reçue il y a X" showing when the alert was created
    return this.formatRelativeTime(notification.createdAt);
  }

  /**
   * Maps NotificationDeliveryStatus to severity for UI styling.
   */
  private mapStatusToSeverity(status: NotificationDeliveryStatus): "low" | "medium" | "high" {
    switch (status) {
      case "FAILED":
        return "high";
      case "PENDING":
        return "medium";
      case "SENT":
      default:
        return "low";
    }
  }

  /**
   * Maps NotificationDeliveryStatus to AlertSummaryItem status.
   */
  private mapDeliveryStatus(status: NotificationDeliveryStatus): "pending" | "sent" | "failed" {
    return status.toLowerCase() as "pending" | "sent" | "failed";
  }

  /**
   * Gets Material icon for notification status.
   */
  private getIconForStatus(status: NotificationDeliveryStatus): string {
    switch (status) {
      case "FAILED":
        return "error";
      case "PENDING":
        return "notifications_active";
      case "SENT":
      default:
        return "check_circle";
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

    if (diffMins < 1) return "À l'instant";
    if (diffMins < 60) return `Il y a ${diffMins} minute${diffMins > 1 ? "s" : ""}`;
    if (diffHours < 24) return `Il y a ${diffHours} heure${diffHours > 1 ? "s" : ""}`;
    return `Il y a ${diffDays} jour${diffDays > 1 ? "s" : ""}`;
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
      (alert) => alert.status === "pending" || alert.status === "failed"
    ).length;

    this.quickActions = [
      {
        title: "Voir la Carte",
        subtitle: "Accéder aux couches de qualité de l'air en temps réel.",
        icon: "map",
        action: "map",
        // TODO: Add mini-map image url here if supported by QuickActionCard
      },
      {
        title: "Mes Notifications & Alertes",
        subtitle:
          actionableAlerts > 0
            ? `${actionableAlerts} nouvelle${actionableAlerts > 1 ? "s" : ""} alerte${
                actionableAlerts > 1 ? "s" : ""
              } en attente`
            : "Aucune nouvelle alerte",
        icon: "notifications_active",
        action: "alerts",
        badge: actionableAlerts ? `${actionableAlerts}` : undefined,
      },
      {
        title: "Aller au Forum",
        subtitle: this.trendingThread ? `Tendance : ${this.trendingThread.title}` : "Discutez avec la communauté",
        icon: "forum",
        action: "forum",
        badge: this.forumUnreadCount ? `${this.forumUnreadCount}` : undefined,
      },
      {
        title: this.isAdmin ? "Stats Système" : "Mes Favoris",
        subtitle: this.isAdmin
          ? "État de santé de l'API et utilisateurs actifs"
          : `${this.userStats.favoriteIndicators} indicateur${
              this.userStats.favoriteIndicators !== 1 ? "s" : ""
            } enregistré${this.userStats.favoriteIndicators !== 1 ? "s" : ""}`,
        icon: this.isAdmin ? "dns" : "favorite",
        action: "favorites",
        badge: !this.isAdmin && this.userStats.favoriteIndicators ? `${this.userStats.favoriteIndicators}` : undefined,
      },
      {
        title: "Historique d'Export",
        subtitle: `Dernier export : ${this.userStats.lastExport}`,
        icon: "download",
        action: "export",
      },
    ];
  }
}
