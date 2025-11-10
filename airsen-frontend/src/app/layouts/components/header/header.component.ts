import { Component, Input, OnInit, OnDestroy } from "@angular/core";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";

import { AuthService } from "@/auth/services/auth.service";
import { AuthUser } from "@/auth/models/auth.model";

@Component({
  standalone: false,
  selector: "app-header",
  templateUrl: "./header.component.html",
  styleUrls: ["./header.component.scss"],
})
export class HeaderComponent implements OnInit, OnDestroy {
  @Input() isAuthenticated = false;

  currentUser: AuthUser | null = null;
  unreadNotifications = 0;
  private destroy$ = new Subject<void>();

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.authService.isAuthenticated$.pipe(takeUntil(this.destroy$)).subscribe((isAuth) => {
      if (isAuth) {
        this.loadNotificationPreview();
      } else {
        this.unreadNotifications = 0;
      }
    });

    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe((user) => {
      this.currentUser = user;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onLogout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(["/"]);
      },
      error: (error) => {
        console.error("Logout error:", error);
        // Navigate anyway for user experience
        this.router.navigate(["/"]);
      },
    });
  }

  navigateToProfile(): void {
    this.router.navigate(["/profile"]);
  }

  navigateToLogin(): void {
    this.router.navigate(["/auth/login"]);
  }

  navigateToAlerts(): void {
    // TODO: Implement map feature to navigate to alerts view
    console.warn("Map feature not yet implemented");
  }

  get userDisplayName(): string {
    return this.currentUser ? `${this.currentUser.firstName} ${this.currentUser.lastName}` : "";
  }

  get userInitials(): string {
    if (!this.currentUser) {
      return "AA";
    }

    const first = this.currentUser.firstName?.trim().charAt(0).toUpperCase() ?? "";
    const last = this.currentUser.lastName?.trim().charAt(0).toUpperCase() ?? "";
    let initials = `${first}${last}`.replace(/\s+/g, "");

    if (!initials) {
      return "AA";
    }

    if (initials.length === 1) {
      return `${initials}${initials}`;
    }

    return initials.slice(0, 2);
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  private loadNotificationPreview(): void {
    // Placeholder implementation until notifications are wired up
    this.unreadNotifications = 3;
  }
}
