import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '@/services/auth.service';
import { StorageService } from '@/services/storage.service';

/**
 * LoginComponent - User authentication interface
 *
 * Features:
 * - Reactive form with email/password validation
 * - Remember me functionality
 * - Return URL support for post-login redirect
 * - Session expiration message handling
 * - Loading states and error handling
 * - Responsive Material Design UI
 */
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private storageService = inject(StorageService);

  loginForm!: FormGroup;
  hidePassword = true;
  sessionExpired = false;
  private destroy$ = new Subject<void>();

  // Observables from AuthService
  isLoading$ = this.authService.isLoading$;
  error$ = this.authService.error$;

  ngOnInit(): void {
    this.initializeForm();
    this.checkSessionExpiration();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize login form with validators
   */
  private initializeForm(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });
  }

  /**
   * Check for session expiration query parameter
   */
  private checkSessionExpiration(): void {
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.sessionExpired = params['sessionExpired'] === 'true';
      });
  }

  /**
   * Handle form submission
   */
  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { email, password } = this.loginForm.value;

    this.authService.login({ email, password })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          // Get return URL or default to dashboard
          const returnUrl = this.storageService.getReturnUrl() || '/dashboard';
          this.storageService.clearReturnUrl();
          this.router.navigate([returnUrl]);
        },
        error: (error) => {
          console.error('Login failed:', error);
          // Error is handled by AuthService and exposed via error$ observable
        }
      });
  }

  /**
   * Navigate to register page
   */
  goToRegister(): void {
    this.router.navigate(['/auth/register']);
  }

  /**
   * Get form field error message
   */
  getErrorMessage(fieldName: string): string {
    const field = this.loginForm.get(fieldName);

    if (!field || !field.touched) {
      return '';
    }

    if (field.hasError('required')) {
      return `${this.getFieldLabel(fieldName)} est requis`;
    }

    if (field.hasError('email')) {
      return 'Veuillez entrer une adresse e-mail valide';
    }

    if (field.hasError('minlength')) {
      const minLength = field.errors?.['minlength'].requiredLength;
      return `Le mot de passe doit contenir au moins ${minLength} caractères`;
    }

    return '';
  }

  /**
   * Get human-readable field label
   */
  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      email: 'L\'adresse e-mail',
      password: 'Le mot de passe'
    };
    return labels[fieldName] || fieldName;
  }

  /**
   * Check if form field has error
   */
  hasError(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }
}
