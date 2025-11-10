import { Component, OnInit, OnDestroy, inject } from "@angular/core";
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from "@angular/forms";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { AuthService } from "@/auth/services/auth.service";
import { RegisterRequest } from "@/auth/models/auth.model";

/**
 * RegisterComponent - User registration interface
 *
 * Features:
 * - Reactive form with comprehensive validation
 * - Password strength requirements
 * - Password confirmation matching
 * - Email format validation
 * - Loading states and error handling
 * - Auto-login after successful registration
 * - Responsive Material Design UI
 */
@Component({
  standalone: false,
  selector: "app-register",
  templateUrl: "./register.component.html",
  styleUrls: ["./register.component.scss"],
})
export class RegisterComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  registerForm!: FormGroup;
  hidePassword = true;
  hideConfirmPassword = true;
  private destroy$ = new Subject<void>();

  // Observables from AuthService
  isLoading$ = this.authService.isLoading$;
  error$ = this.authService.error$;

  ngOnInit(): void {
    this.initializeForm();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize registration form with validators
   */
  private initializeForm(): void {
    this.registerForm = this.fb.group(
      {
        firstName: ["", [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        lastName: ["", [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        email: ["", [Validators.required, Validators.email]],
        password: ["", [Validators.required, Validators.minLength(8), this.passwordStrengthValidator]],
        confirmPassword: ["", [Validators.required]],
        acceptTerms: [false, [Validators.requiredTrue]],
      },
      {
        validators: this.passwordMatchValidator,
      }
    );

    // Clear error when user starts typing in any form field
    this.registerForm.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.authService.clearError();
    });
  }

  /**
   * Custom validator for password strength
   */
  private passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;

    if (!value) {
      return null;
    }

    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumeric = /[0-9]/.test(value);
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(value);

    const passwordValid = hasUpperCase && hasLowerCase && hasNumeric && hasSpecialChar;

    return !passwordValid ? { passwordStrength: true } : null;
  }

  /**
   * Custom validator for password confirmation matching
   */
  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get("password")?.value;
    const confirmPassword = group.get("confirmPassword")?.value;

    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  /**
   * Handle form submission
   */
  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const { firstName, lastName, email, password } = this.registerForm.value;

    const registerRequest: RegisterRequest = {
      firstName,
      lastName,
      email,
      password,
    };

    this.authService
      .register(registerRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          // TODO: Redirect to map feature when implemented
          console.warn("Map feature not yet implemented");
          // Registration successful
        },
        error: (error) => {
          console.error("Registration failed:", error);
          // Error is handled by AuthService and exposed via error$ observable
        },
      });
  }

  /**
   * Navigate to login page
   */
  goToLogin(): void {
    this.router.navigate(["/auth/login"]);
  }

  /**
   * Get form field error message
   */
  getErrorMessage(fieldName: string): string {
    const field = this.registerForm.get(fieldName);

    if (!field || !field.touched) {
      return "";
    }

    if (field.hasError("required")) {
      return `${this.getFieldLabel(fieldName)} est requis`;
    }

    if (field.hasError("email")) {
      return "Veuillez entrer une adresse e-mail valide";
    }

    if (field.hasError("minlength")) {
      const minLength = field.errors?.["minlength"].requiredLength;
      return `Doit contenir au moins ${minLength} caractères`;
    }

    if (field.hasError("maxlength")) {
      const maxLength = field.errors?.["maxlength"].requiredLength;
      return `Ne doit pas dépasser ${maxLength} caractères`;
    }

    if (field.hasError("passwordStrength")) {
      return "Le mot de passe doit contenir une majuscule, une minuscule, un chiffre et un caractère spécial";
    }

    return "";
  }

  /**
   * Get password confirmation error message
   */
  getConfirmPasswordError(): string {
    const field = this.registerForm.get("confirmPassword");

    if (!field || !field.touched) {
      return "";
    }

    if (field.hasError("required")) {
      return "Veuillez confirmer votre mot de passe";
    }

    if (this.registerForm.hasError("passwordMismatch")) {
      return "Les mots de passe ne correspondent pas";
    }

    return "";
  }

  /**
   * Get human-readable field label
   */
  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      firstName: "Le prénom",
      lastName: "Le nom",
      email: "L'adresse e-mail",
      password: "Le mot de passe",
      confirmPassword: "La confirmation du mot de passe",
    };
    return labels[fieldName] || fieldName;
  }

  /**
   * Check if form field has error
   */
  hasError(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /**
   * Check if confirm password has error
   */
  hasConfirmPasswordError(): boolean {
    const field = this.registerForm.get("confirmPassword");
    return !!(field && field.touched && (field.invalid || this.registerForm.hasError("passwordMismatch")));
  }
}
