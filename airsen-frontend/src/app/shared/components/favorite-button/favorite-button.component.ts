import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';

import { FavoriteService } from '@/features/favorites/services/favorite.service';
import { AuthService } from '@/core/auth/services/auth.service';

/**
 * FavoriteButtonComponent
 *
 * Commune-only favorite toggle button
 *
 * Example usage:
 * <app-favorite-button
 *   [communeInseeCode]="'75056'"
 *   size="medium"
 *   (favoriteToggled)="onFavoriteToggled($event)">
 * </app-favorite-button>
 *
 * <div class="commune-card">
 *   <h3>{{ commune.name }}</h3>
 *   <p>{{ commune.departmentName }}</p>
 *   <app-favorite-button
 *     [communeInseeCode]="commune.inseeCode"
 *     size="small">
 *   </app-favorite-button>
 * </div>
 */
@Component({
  standalone: false,
  selector: 'app-favorite-button',
  templateUrl: './favorite-button.component.html',
  styleUrls: ['./favorite-button.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FavoriteButtonComponent implements OnInit, OnDestroy {
  @Input() communeInseeCode!: string; // 5-digit INSEE code (REQUIRED)
  @Input() size: 'small' | 'medium' | 'large' = 'medium';
  @Output() favoriteToggled = new EventEmitter<boolean>(); // true = added, false = removed

  isFavorited = false;
  isLoading = false;
  isMaxReached = false;
  currentUserId?: number;
  animateSuccess = false;

  private readonly destroy$ = new Subject<void>();

  private favoriteService = inject(FavoriteService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    // Validate required input in strict mode
    if (!this.communeInseeCode || !/^\d{5}$/.test(this.communeInseeCode)) {
      // Fail silently but log for dev; button stays disabled via guard
      console.warn('FavoriteButtonComponent: invalid or missing communeInseeCode');
    }

    // Get current user ID and initialize favorite status
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUserId = user?.id;
        if (this.currentUserId && this.communeInseeCode) {
          this.checkInitialFavorite();
          this.refreshMaxReached();
        }
        this.cdr.markForCheck();
      });

    // Track favorite count to know if maximum (10) is reached
    this.favoriteService.favoriteCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe(count => {
        this.isMaxReached = count.count >= count.maximum;
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleFavorite(): void {
    if (!this.currentUserId || !this.communeInseeCode) {
      this.openSnack('Veuillez vous connecter pour gérer vos favoris.', true);
      return;
    }

    // Guard: prevent adding when at maximum
    if (!this.isFavorited && this.isMaxReached) {
      this.openSnack('Limite atteinte : 10 favoris maximum', true);
      return;
    }

    const previous = this.isFavorited;
    this.isLoading = true;
    // Optimistic UI update
    this.isFavorited = !this.isFavorited;
    this.cdr.markForCheck();

    this.favoriteService.toggleFavorite(this.currentUserId, this.communeInseeCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (added) => {
          // Service returns true when added, false when removed
          this.isLoading = false;

          // Update state based on actual service response
          this.isFavorited = added;

          // Trigger success animation only on add
          if (added) {
            this.animateSuccess = true;
            setTimeout(() => {
              this.animateSuccess = false;
              this.cdr.markForCheck();
            }, 300);

            const count = this.favoriteService.getCurrentCount();
            this.openSnack(`Ajouté aux favoris (${count.count}/${count.maximum})`);
          } else {
            this.openSnack('Retiré des favoris');
          }

          this.favoriteToggled.emit(added);
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          // Rollback optimistic state and show error
          this.isLoading = false;
          this.isFavorited = previous;

          // Check if error is about maximum favorites limit
          let message = 'Une erreur est survenue lors de la gestion des favoris';
          if (err instanceof Error) {
            if (err.message.toLowerCase().includes('maximum') ||
                err.message.toLowerCase().includes('limit')) {
              message = 'Limite atteinte : 10 favoris maximum';
            } else if (err.message.toLowerCase().includes('already exists')) {
              message = 'Ce favori existe déjà';
            } else {
              message = err.message;
            }
          }

          this.openSnack(message, true);
          this.cdr.markForCheck();
        }
      });
  }

  private checkInitialFavorite(): void {
    if (!this.currentUserId || !this.communeInseeCode) return;
    this.isLoading = true;
    this.cdr.markForCheck();

    this.favoriteService.checkFavorite(this.currentUserId, this.communeInseeCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.isFavorited = !!res?.isFavorited;
          this.isLoading = false;
          this.cdr.markForCheck();
        },
        error: (err: unknown) => {
          this.isLoading = false;
          const message = err instanceof Error
            ? err.message
            : 'Impossible de vérifier le statut du favori';
          this.openSnack(message, true);
          this.cdr.markForCheck();
        }
      });
  }

  private refreshMaxReached(): void {
    const count = this.favoriteService.getCurrentCount();
    this.isMaxReached = count.count >= count.maximum;
  }

  private openSnack(message: string, isError: boolean = false): void {
    // Error messages stay longer (5s) so users can read them
    // Success messages disappear faster (3.5s)
    const duration = isError ? 5000 : 3500;
    this.snackBar.open(message, 'Fermer', {
      duration,
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }
}
