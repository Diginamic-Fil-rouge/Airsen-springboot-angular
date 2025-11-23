import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';

import { UserFavoriteResponse, FavoriteCountResponse } from '@/shared/models/favorite.model';
import { FavoriteService } from '@/features/favorites/services/favorite.service';
import { ExportDataService } from '@/services/export-data.service';
import { AuthService } from '@/auth/services/auth.service';
import { ExportHistoryComponent } from '../../components/export-history/export-history.component';

interface ExportPageState {
  favorites: UserFavoriteResponse[];
  favoriteCount: FavoriteCountResponse;
  isLoading: boolean;
  error: string | null;
  currentUserId: number;
  selectedCommune: UserFavoriteResponse | null;
  showCsvPanel: boolean;
}

@Component({
  standalone: false,
  selector: 'app-export',
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExportPageComponent implements OnInit, OnDestroy {
  @ViewChild(ExportHistoryComponent) exportHistoryComponent!: ExportHistoryComponent;

  private destroy$ = new Subject<void>();

  state: ExportPageState = {
    favorites: [],
    favoriteCount: { count: 0, maximum: 10 },
    isLoading: true,
    error: null,
    currentUserId: 0,
    selectedCommune: null,
    showCsvPanel: false
  };

  constructor(
    private favoriteService: FavoriteService,
    private exportDataService: ExportDataService,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
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

  onSelectCommuneForExport(commune: UserFavoriteResponse): void {
    this.state.selectedCommune = commune;
    this.state.showCsvPanel = true;
    this.cdr.markForCheck();
  }

  onCsvExportComplete(): void {
    this.state.showCsvPanel = false;
    this.state.selectedCommune = null;
    // Refresh export history to show the new export
    if (this.exportHistoryComponent) {
      this.exportHistoryComponent.loadExportHistory();
    }
    this.cdr.markForCheck();
    this.showSuccess('Export CSV completed successfully');
  }

  onCsvExportCancelled(): void {
    this.state.showCsvPanel = false;
    this.state.selectedCommune = null;
    this.cdr.markForCheck();
  }

  viewCommuneDetails(communeInseeCode: string): void {
    this.router.navigate(['/map'], {
      queryParams: { commune: communeInseeCode, openSidebar: 'true' }
    });
  }

  browseCommunes(): void {
    this.router.navigate(['/map']);
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