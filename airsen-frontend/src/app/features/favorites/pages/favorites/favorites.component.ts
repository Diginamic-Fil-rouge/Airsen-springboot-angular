import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';

import { UserFavoriteResponse, FavoriteCountResponse } from '@/shared/models/favorite.model';
import { FavoriteService } from '../../services/favorite.service';
import { AuthService } from '@/core/auth/services/auth.service';
import { ConfirmDialogComponent } from '@/shared/components/confirm-dialog/confirm-dialog.component';

interface FavoritesPageState {
  favorites: UserFavoriteResponse[];
  favoriteCount: FavoriteCountResponse;
  isLoading: boolean;
  error: string | null;
  currentUserId: number;
}

@Component({
  standalone: false,
  selector: 'app-favorites',
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FavoritesComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  state: FavoritesPageState = {
    favorites: [],
    favoriteCount: { count: 0, maximum: 10 },
    isLoading: true,
    error: null,
    currentUserId: 0
  };

  constructor(
    private favoriteService: FavoriteService,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  primaryFavoriteInseeCode: string | null = null;

  ngOnInit(): void {
    this.primaryFavoriteInseeCode = localStorage.getItem('primaryFavoriteInseeCode');
    this.initializeComponent();
  }

  // ... existing methods ...

  setPrimary(communeInseeCode: string): void {
    if (this.primaryFavoriteInseeCode === communeInseeCode) {
      this.primaryFavoriteInseeCode = null;
      localStorage.removeItem('primaryFavoriteInseeCode');
      
      const defaultFavorite = this.state.favorites[0];
      const defaultName = defaultFavorite ? defaultFavorite.communeName : 'première de la liste';
      this.showSuccess(`Commune principale réinitialisée (par défaut : ${defaultName})`);
    } else {
      this.primaryFavoriteInseeCode = communeInseeCode;
      localStorage.setItem('primaryFavoriteInseeCode', communeInseeCode);
      
      const favorite = this.state.favorites.find(f => f.communeInseeCode === communeInseeCode);
      if (favorite) {
        this.showSuccess(`${favorite.communeName} définie comme commune principale`);
      }
    }
    this.cdr.markForCheck();
  }
  
  isPrimary(communeInseeCode: string): boolean {
      return this.primaryFavoriteInseeCode === communeInseeCode;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeComponent(): void {
    // Get current user
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      this.handleError('User not authenticated');
      return;
    }

    this.state.currentUserId = currentUser.id;

    // Subscribe to favorites and count updates
    this.favoriteService.favorites$
      .pipe(takeUntil(this.destroy$))
      .subscribe(favorites => {
        this.state.favorites = favorites;
        this.cdr.markForCheck();
      });

    this.favoriteService.favoriteCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe(count => {
        this.state.favoriteCount = count;
        this.cdr.markForCheck();
      });

    this.favoriteService.loading$
      .pipe(takeUntil(this.destroy$))
      .subscribe(loading => {
        this.state.isLoading = loading;
        this.cdr.markForCheck();
      });

    this.favoriteService.error$
      .pipe(takeUntil(this.destroy$))
      .subscribe(error => {
        this.state.error = error;
        this.cdr.markForCheck();
      });

    // Load initial data
    this.loadFavorites();
  }

  private loadFavorites(): void {
    this.favoriteService.getUserFavorites(this.state.currentUserId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        error: (error) => {
          console.error('Failed to load favorites:', error);
          this.showError('Failed to load favorites. Please try again.');
        }
      });
  }

  removeFavorite(communeInseeCode: string): void {
    const favorite = this.state.favorites.find(f => f.communeInseeCode === communeInseeCode);
    if (!favorite) return;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: {
        title: 'Remove from favorites',
        message: `Remove ${favorite.communeName} from your favorites?`,
        confirmButtonText: 'Remove',
        cancelButtonText: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.performRemoveFavorite(communeInseeCode, favorite.communeName);
      }
    });
  }

  private performRemoveFavorite(communeInseeCode: string, communeName: string): void {
    this.favoriteService.removeFavorite(this.state.currentUserId, communeInseeCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showSuccess(`${communeName} removed from favorites (${this.state.favoriteCount.count}/${this.state.favoriteCount.maximum})`);
        },
        error: (error) => {
          console.error('Failed to remove favorite:', error);
          this.showError('Failed to remove favorite. Please try again.');
          // Rollback will be handled by the service
        }
      });
  }

  viewCommuneDetails(communeInseeCode: string): void {
    this.router.navigate(['/map'], {
      queryParams: { commune: communeInseeCode, openSidebar: 'true' }
    });
  }

  browseCommunes(): void {
    this.router.navigate(['/communes']);
  }

  retryLoad(): void {
    this.state.error = null;
    this.loadFavorites();
  }

  private handleError(message: string): void {
    this.state.error = message;
    this.state.isLoading = false;
    this.cdr.markForCheck();
  }

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['success-snackbar']
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: 5000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['error-snackbar']
    });
  }
}