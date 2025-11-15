import { Component, OnInit, HostListener, OnDestroy } from "@angular/core";
import { Router, NavigationEnd } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil, filter } from "rxjs/operators";
import { AuthService } from "@/auth/services/auth.service";
import { SidebarService } from "@/shared/services/sidebar.service";

/**
 * Navigation item interface with optional role restrictions and badge count
 */
interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[]; // Optional: if specified, only users with these roles can see this item
  featured?: boolean; // Optional: highlight this item with special styling
  badgeCount?: number; // Optional: notification count badge (Alertes, Favoris, etc.)
}

/**
 * Sidebar Component
 *
 * Collapsible navigation sidebar with user profile, role-based navigation,
 * and responsive behavior (overlay on mobile/tablet).
 */
@Component({
  standalone: false,
  selector: "app-sidebar",
  templateUrl: "./sidebar.component.html",
  styleUrls: ["./sidebar.component.scss"],
})
export class SidebarComponent implements OnInit, OnDestroy {
  isExpanded = true;

  /** Show backdrop for overlay mode (mobile/tablet) */
  showBackdrop = false;

  /** Current user's full name */
  userName = "";

  /** Current user's email address */
  userEmail = "";

  /** User's initial for avatar display */
  userInitial = "";

  /** Current active route */
  currentRoute = "";

  /** Subject for cleanup on destroy */
  private destroy$ = new Subject<void>();

  /** Application version */
  appVersion = "1.2.0";

  /** Sidebar width in pixels */
  sidebarWidth = 240;

  /**
   * Navigation items with role-based access control
   * Items without roles are visible to all authenticated users
   */
  navItems: NavItem[] = [
    // Main Navigation Section
    { label: "Accueil", icon: "home", route: "/dashboard" },
    { label: "Carte", icon: "map", route: "/map" },
    { label: "Forum", icon: "forum", route: "/forum" },
    { label: "Favoris", icon: "favorite", route: "/favorites" },
    { label: "Exports", icon: "download", route: "/export" },
    { label: "Profil", icon: "person", route: "/profile" },
    { label: "Paramètres", icon: "settings", route: "/settings" },

    // Admin Section - only visible to ADMIN users
    { label: "Tableau de Bord", icon: "dashboard", route: "/admin/dashboard", roles: ["ADMIN"] },
    { label: "Utilisateurs", icon: "group", route: "/admin/users", roles: ["ADMIN"] },
    { label: "Signaux d'Alerte", icon: "notification_important", route: "/admin/alerts", roles: ["ADMIN"] },
    { label: "Campagnes", icon: "campaign", route: "/admin/campaigns", roles: ["ADMIN"] },
    { label: "Journal d'Audit", icon: "assignment", route: "/admin/audit", roles: ["ADMIN"] },
  ];

  constructor(
    private authService: AuthService,
    private router: Router,
    private sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.loadUserData();
    this.loadSidebarState();
    this.updateBackdropVisibility();
    this.setupRouterEvents();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load current user data from authentication service
   */
  private loadUserData(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userName = `${user.firstName} ${user.lastName}`.trim() || "User Name";
      this.userEmail = user.email || "";
      this.userInitial = user.firstName?.charAt(0).toUpperCase() || "U";
    }
  }

  /**
   * Load sidebar expansion state from SidebarService
   */
  private loadSidebarState(): void {
    this.isExpanded = this.sidebarService.getSidebarState();
    // Listen to sidebar state changes
    this.sidebarService.sidebarExpanded$
      .pipe(takeUntil(this.destroy$))
      .subscribe((isExpanded: boolean) => {
        this.isExpanded = isExpanded;
        this.updateBackdropVisibility();
      });
  }

  /**
   * Toggle sidebar expansion state
   * Uses SidebarService to broadcast state changes globally
   */
  toggleSidebar(): void {
    this.sidebarService.toggleSidebar();
  }

  /**
   * Handle user logout
   * Clears authentication state and redirects to login
   */
  onLogout(): void {
    this.authService
      .logout()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.router.navigate(["auth//login"]);
        },
        error: (error) => {
          console.error("Logout error:", error);
          // Navigate to login even if logout request fails
          this.router.navigate(["auth/login"]);
        },
      });
  }

  /**
   * Handle window resize events
   * Updates backdrop visibility based on screen size
   */
  @HostListener("window:resize")
  onResize(): void {
    this.updateBackdropVisibility();
  }

  /**
   * Update backdrop visibility based on screen width
   * Show backdrop only on mobile/tablet (< 1024px) when expanded
   */
  private updateBackdropVisibility(): void {
    this.showBackdrop = window.innerWidth < 1024 && this.isExpanded;
  }

  /**
   * Setup router event subscription for active route tracking
   */
  private setupRouterEvents(): void {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.urlAfterRedirects;
      });

    // Set initial route
    this.currentRoute = this.router.url;
  }

  /**
   * Check if a navigation item should be visible based on user roles
   * @param navItem Navigation item to check
   * @returns true if item should be visible, false otherwise
   */
  shouldShowNavItem(navItem: NavItem): boolean {
    // If no roles specified, item is visible to all authenticated users
    if (!navItem.roles || navItem.roles.length === 0) {
      return true;
    }

    // Check if user has at least one of the required roles
    return navItem.roles.some((role) => this.authService.hasRole(role));
  }

  /**
   * Get list of visible main navigation items (non-admin)
   * @returns Array of main navigation items visible to current user
   */
  get mainNavItems(): NavItem[] {
    return this.navItems
      .filter((item) => !item.roles || !item.roles.includes("ADMIN"))
      .filter((item) => this.shouldShowNavItem(item));
  }

  /**
   * Get list of visible admin navigation items
   * @returns Array of admin navigation items visible to current user
   */
  get adminNavItems(): NavItem[] {
    return this.navItems
      .filter((item) => item.roles && item.roles.includes("ADMIN"))
      .filter((item) => this.shouldShowNavItem(item));
  }

  /**
   * Get list of all visible navigation items (for backward compatibility)
   * @returns Array of navigation items visible to current user
   */
  get visibleNavItems(): NavItem[] {
    return this.navItems.filter((item) => this.shouldShowNavItem(item));
  }

  /**
   * Check if a route is currently active
   * @param route Route to check
   * @returns true if route is active, false otherwise
   */
  isActive(route: string): boolean {
    return this.currentRoute.startsWith(route);
  }
}
