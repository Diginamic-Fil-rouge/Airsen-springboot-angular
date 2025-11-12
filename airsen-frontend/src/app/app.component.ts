import { Component, OnInit, OnDestroy } from "@angular/core";
import { Router, NavigationEnd } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil, filter } from "rxjs/operators";

import { AuthService } from "./core/auth/services/auth.service";
import { SidebarService } from "./shared/services/sidebar.service";

// TODO: Uncomment when services are implemented
// import { NotificationService } from './services/notification.service';

@Component({
  standalone: false,
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.scss"],
})
export class AppComponent implements OnInit, OnDestroy {
  title = "Airsen - Plateforme de Qualité de l'Air";
  isAuthenticated = false;
  isSidebarExpanded = true;
  isLoading = false;
  currentRoute = "";

  private destroy$ = new Subject<void>();

  constructor(
    // TODO: Add services when they are implemented
    private authService: AuthService,
    private sidebarService: SidebarService,
    // private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeApp();
    this.setupRouterEvents();
    // TODO: Uncomment when AuthService is implemented
    this.setupAuthStateListener();
    this.setupSidebarStateListener();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeApp(): void {
    this.isLoading = true;

    // TODO: Initialize authentication state when AuthService is implemented
    // this.authService.initializeAuthState()
    //   .pipe(takeUntil(this.destroy$))
    //   .subscribe({
    //     next: () => {
    //       this.isLoading = false;
    //     },
    //     error: (error) => {
    //       console.error('App initialization error:', error);
    //       this.isLoading = false;
    //     }
    //   });

    // Temporary: Set loading to false immediately
    this.isLoading = false;
  }

  private setupRouterEvents(): void {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event: NavigationEnd) => {
        this.currentRoute = event.urlAfterRedirects;
      });
  }

  // TODO: Uncomment when AuthService is implemented
  private setupAuthStateListener(): void {
    this.authService.isAuthenticated$.pipe(takeUntil(this.destroy$)).subscribe((isAuth) => {
      this.isAuthenticated = isAuth;
    });
  }

  get shouldShowNavigation(): boolean {
    const noNavRoutes = ["/login", "/register", "/forgot-password", "/reset-password"];
    return !noNavRoutes.some((route) => this.currentRoute.startsWith(route));
  }

  get shouldShowBreadcrumb(): boolean {
    const noBreadcrumbRoutes = ["/", "/login", "/register", "/forgot-password"];
    return !noBreadcrumbRoutes.includes(this.currentRoute);
  }

  /**
   * Setup sidebar state listener to update layout when sidebar is toggled
   */
  private setupSidebarStateListener(): void {
    this.sidebarService.sidebarExpanded$.pipe(takeUntil(this.destroy$)).subscribe((isExpanded: boolean) => {
      this.isSidebarExpanded = isExpanded;
    });
  }
}
