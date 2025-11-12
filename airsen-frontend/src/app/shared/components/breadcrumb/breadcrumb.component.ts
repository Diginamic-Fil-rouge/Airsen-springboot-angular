import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';

export interface Breadcrumb {
  label: string;
  url: string;
  isActive: boolean;
}

@Component({
  standalone: false,
  selector: 'app-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.scss']
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  breadcrumbs: Breadcrumb[] = [];
  private destroy$ = new Subject<void>();

  // Mapping of routes to friendly labels
  private readonly routeLabelMap: Record<string, string> = {
    'dashboard': 'Tableau de bord',
    'map': 'Carte interactive',
    'favorites': 'Favoris',
    'history': 'Historique',
    'forum': 'Forum',
    'alerts': 'Alertes',
    'profile': 'Profil',
    'settings': 'Paramètres',
    'admin': 'Administration',
    'users': 'Utilisateurs',
    'categories': 'Catégories',
    'threads': 'Discussions',
    'messages': 'Messages',
    'about': 'À propos',
    'help': 'Aide',
    'contact': 'Contact',
    'legal': 'Mentions légales',
    'terms': 'Conditions d\'utilisation',
    'privacy': 'Confidentialité',
    'edit': 'Modifier',
    'create': 'Créer',
    'view': 'Voir'
  };

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.breadcrumbs = this.createBreadcrumbs(this.activatedRoute.root);
      });

    // Initial breadcrumbs
    this.breadcrumbs = this.createBreadcrumbs(this.activatedRoute.root);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private createBreadcrumbs(
    route: ActivatedRoute,
    url = '',
    breadcrumbs: Breadcrumb[] = []
  ): Breadcrumb[] {
    const children: ActivatedRoute[] = route.children;

    if (children.length === 0) {
      return breadcrumbs;
    }

    for (const child of children) {
      const routeURL: string = child.snapshot.url.map(segment => segment.path).join('/');

      if (routeURL !== '') {
        url += `/${routeURL}`;

        // Get label from route data or use default mapping
        const label = child.snapshot.data['breadcrumb'] || this.getLabel(routeURL);

        breadcrumbs.push({
          label,
          url,
          isActive: false
        });
      }

      return this.createBreadcrumbs(child, url, breadcrumbs);
    }

    // Mark last breadcrumb as active
    if (breadcrumbs.length > 0) {
      breadcrumbs[breadcrumbs.length - 1].isActive = true;
    }

    return breadcrumbs;
  }

  private getLabel(route: string): string {
    const segments = route.split('/');
    const lastSegment = segments[segments.length - 1];

    // Check if it's a known route
    if (this.routeLabelMap[lastSegment]) {
      return this.routeLabelMap[lastSegment];
    }

    // If it's a number, it might be an ID, return a generic label
    if (!isNaN(Number(lastSegment))) {
      return 'Détails';
    }

    // Capitalize first letter
    return lastSegment.charAt(0).toUpperCase() + lastSegment.slice(1);
  }

  navigateTo(url: string): void {
    this.router.navigate([url]);
  }
}
