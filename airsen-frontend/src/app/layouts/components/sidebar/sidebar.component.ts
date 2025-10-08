import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';

import { AuthService } from '@/auth/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[];
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit, OnDestroy {
  isCollapsed = false;
  currentRoute = '';
  private destroy$ = new Subject<void>();

  navItems: NavItem[] = [
    { label: 'Tableau de bord', icon: 'dashboard', route: '/dashboard' },
    { label: 'Carte', icon: 'map', route: '/map' },
    { label: 'Favoris', icon: 'favorite', route: '/favorites' },
    { label: 'Historique', icon: 'history', route: '/history' },
    { label: 'Forum', icon: 'forum', route: '/forum' },
    { label: 'Alertes', icon: 'notifications', route: '/alerts' },
    { label: 'Profil', icon: 'person', route: '/profile' },
    { label: 'Administration', icon: 'admin_panel_settings', route: '/admin', roles: ['ADMIN'] }
  ];

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.setupRouterEvents();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  isActive(route: string): boolean {
    return this.currentRoute.startsWith(route);
  }

  shouldShowNavItem(navItem: NavItem): boolean {
    if (!navItem.roles || navItem.roles.length === 0) {
      return true;
    }
    // Check if user has at least one of the required roles
    return navItem.roles.some(role => this.authService.hasRole(role));
  }

  get visibleNavItems(): NavItem[] {
    return this.navItems.filter(item => this.shouldShowNavItem(item));
  }
}
