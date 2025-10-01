import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

/**
 * HomeComponent - Page d'accueil de la plateforme Airsen
 *
 * Affiche:
 * - Hero section avec recherche de commune
 * - Statistiques de la plateforme
 * - Fonctionnalités principales
 * - Témoignages d'utilisateurs
 */
@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {

  constructor(private router: Router) { }

  ngOnInit(): void {
    // Component initialization
  }

  /**
   * Navigate to login page
   */
  goToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  /**
   * Navigate to map page
   */
  goToMap(): void {
    // TODO: Navigate to map when implemented
    console.log('Navigate to map');
  }

  /**
   * Navigate to forum page
   */
  goToForum(): void {
    // TODO: Navigate to forum when implemented
    console.log('Navigate to forum');
  }

}
