import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ChangePasswordRequest, PasswordStrengthResult } from '@/shared/models/profile.model';

/**
 * ChangePasswordComponent - Password Change Form
 *
 * Reactive form with advanced password validation:
 * - Current password (required)
 * - New password (required, min 8 chars, strength validation)
 * - Confirm password (required, must match newPassword)
 *
 * Features:
 * - Real-time password strength indicator (WEAK/FAIR/GOOD/STRONG)
 * - Show/hide password toggles
 * - Visual color-coded strength meter
 * - Custom validators: passwordMatch, passwordStrength, differentFromCurrent
 *
 * Emits passwordChange event when form is submitted successfully.
 */
@Component({
  standalone: false,
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss']
})
export class ChangePasswordComponent implements OnInit {
  @Output() passwordChange = new EventEmitter<ChangePasswordRequest>();

  passwordForm!: FormGroup;
  isSubmitting = false;

  // Show/hide password states
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  // Password strength
  passwordStrength: PasswordStrengthResult | null = null;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  /**
   * Initializes reactive form with custom validators.
   */
  private initializeForm(): void {
    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required, Validators.minLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(8), this.passwordStrengthValidator.bind(this)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: [this.passwordsMatchValidator, this.differentFromCurrentValidator]
    });

    // Subscribe to newPassword changes to update strength indicator
    this.passwordForm.get('newPassword')?.valueChanges.subscribe(password => {
      this.updatePasswordStrength(password);
    });
  }

  /**
   * Custom validator: Checks if newPassword and confirmPassword match.
   */
  private passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (confirmPassword && newPassword !== confirmPassword) {
      control.get('confirmPassword')?.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }

    // Clear error if passwords match
    const errors = control.get('confirmPassword')?.errors;
    if (errors && errors['passwordMismatch']) {
      delete errors['passwordMismatch'];
      control.get('confirmPassword')?.setErrors(Object.keys(errors).length > 0 ? errors : null);
    }

    return null;
  }

  /**
   * Custom validator: Checks if newPassword is different from currentPassword.
   */
  private differentFromCurrentValidator(control: AbstractControl): ValidationErrors | null {
    const currentPassword = control.get('currentPassword')?.value;
    const newPassword = control.get('newPassword')?.value;

    if (currentPassword && newPassword && currentPassword === newPassword) {
      control.get('newPassword')?.setErrors({ sameAsCurrent: true });
      return { sameAsCurrent: true };
    }

    return null;
  }

  /**
   * Custom validator: Validates password strength (min FAIR required).
   */
  private passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.value;

    if (!password) return null;

    const strength = this.calculatePasswordStrength(password);

    if (strength.strength === 'WEAK') {
      return { weakPassword: true };
    }

    return null;
  }

  /**
   * Calculates password strength score and returns detailed result.
   */
  private calculatePasswordStrength(password: string): PasswordStrengthResult {
    if (!password) {
      return {
        strength: 'WEAK',
        score: 0,
        color: '#f44336',
        message: 'Aucun mot de passe saisi',
        hasMinLength: false,
        hasUpperCase: false,
        hasLowerCase: false,
        hasNumbers: false,
        hasSpecialChars: false
      };
    }

    const hasMinLength = password.length >= 8;
    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumbers = /[0-9]/.test(password);
    const hasSpecialChars = /[!@#$%^&*(),.?":{}|<>]/.test(password);

    // Calculate score (0-100)
    let score = 0;
    if (hasMinLength) score += 25;
    if (hasUpperCase) score += 20;
    if (hasLowerCase) score += 20;
    if (hasNumbers) score += 20;
    if (hasSpecialChars) score += 15;

    // Determine strength level
    let strength: 'WEAK' | 'FAIR' | 'GOOD' | 'STRONG';
    let color: string;
    let message: string;

    if (score < 40) {
      strength = 'WEAK';
      color = '#f44336'; // Red
      message = 'Mot de passe faible';
    } else if (score < 60) {
      strength = 'FAIR';
      color = '#ff9800'; // Orange
      message = 'Mot de passe moyen';
    } else if (score < 85) {
      strength = 'GOOD';
      color = '#2196f3'; // Blue
      message = 'Bon mot de passe';
    } else {
      strength = 'STRONG';
      color = '#4caf50'; // Green
      message = 'Excellent mot de passe';
    }

    return {
      strength,
      score,
      color,
      message,
      hasMinLength,
      hasUpperCase,
      hasLowerCase,
      hasNumbers,
      hasSpecialChars
    };
  }

  /**
   * Updates password strength indicator.
   */
  private updatePasswordStrength(password: string): void {
    this.passwordStrength = this.calculatePasswordStrength(password);
  }

  /**
   * Handles form submission.
   */
  onSubmit(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    const request: ChangePasswordRequest = {
      currentPassword: this.passwordForm.value.currentPassword,
      newPassword: this.passwordForm.value.newPassword,
      confirmPassword: this.passwordForm.value.confirmPassword
    };

    this.passwordChange.emit(request);

    // Reset submitting state after emission
    setTimeout(() => {
      this.isSubmitting = false;
    }, 500);
  }

  /**
   * Resets form to initial state.
   */
  onCancel(): void {
    this.passwordForm.reset();
    this.passwordStrength = null;
  }

  /**
   * Toggles current password visibility.
   */
  toggleCurrentPasswordVisibility(): void {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  /**
   * Toggles new password visibility.
   */
  toggleNewPasswordVisibility(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  /**
   * Toggles confirm password visibility.
   */
  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  /**
   * Gets error message for currentPassword field.
   */
  get currentPasswordError(): string {
    const control = this.passwordForm.get('currentPassword');
    if (control?.hasError('required')) return 'Le mot de passe actuel est requis';
    if (control?.hasError('minlength')) return 'Le mot de passe actuel doit contenir au moins 6 caractères';
    return '';
  }

  /**
   * Gets error message for newPassword field.
   */
  get newPasswordError(): string {
    const control = this.passwordForm.get('newPassword');
    if (control?.hasError('required')) return 'Le nouveau mot de passe est requis';
    if (control?.hasError('minlength')) return 'Le nouveau mot de passe doit contenir au moins 8 caractères';
    if (control?.hasError('weakPassword')) return 'Le mot de passe est trop faible. Ajoutez des majuscules, chiffres ou symboles.';
    if (control?.hasError('sameAsCurrent')) return 'Le nouveau mot de passe doit être différent de l\'actuel';
    return '';
  }

  /**
   * Gets error message for confirmPassword field.
   */
  get confirmPasswordError(): string {
    const control = this.passwordForm.get('confirmPassword');
    if (control?.hasError('required')) return 'La confirmation du mot de passe est requise';
    if (control?.hasError('passwordMismatch')) return 'Les mots de passe ne correspondent pas';
    return '';
  }
}
