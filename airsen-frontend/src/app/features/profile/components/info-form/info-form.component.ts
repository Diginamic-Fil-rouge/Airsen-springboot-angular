import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserProfile, UpdateProfileRequest } from '@/shared/models/profile.model';

/**
 * InfoFormComponent - Personal Information Form
 *
 * Reactive form for editing user profile:
 * - First Name (required, 2-50 chars)
 * - Last Name (required, 2-50 chars)
 * - Telephone (optional, French format: 10 digits)
 * - Address (optional, max 200 chars)
 * - Bio (optional, max 500 chars)
 *
 * Emits profileUpdate event when form is submitted successfully.
 */
@Component({
  standalone: false,
  selector: 'app-info-form',
  templateUrl: './info-form.component.html',
  styleUrls: ['./info-form.component.scss']
})
export class InfoFormComponent implements OnInit {
  @Input() user: UserProfile | null = null;
  @Output() profileUpdate = new EventEmitter<UpdateProfileRequest>();

  profileForm!: FormGroup;
  isSubmitting = false;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  /**
   * Initializes reactive form with validation rules.
   */
  private initializeForm(): void {
    this.profileForm = this.fb.group({
      firstName: [
        this.user?.firstName || '',
        [Validators.required, Validators.minLength(2), Validators.maxLength(50)]
      ],
      lastName: [
        this.user?.lastName || '',
        [Validators.required, Validators.minLength(2), Validators.maxLength(50)]
      ],
      telephone: [
        this.user?.telephone || '',
        [Validators.pattern(/^(?:(?:\+|00)33|0)\s*[1-9](?:[\s.-]*\d{2}){4}$/)]
      ],
      address: [
        this.user?.address || '',
        [Validators.maxLength(200)]
      ],
      bio: [
        this.user?.bio || '',
        [Validators.maxLength(500)]
      ]
    });
  }

  /**
   * Handles form submission.
   * Validates form and emits profileUpdate event with UpdateProfileRequest.
   */
  onSubmit(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    const updateRequest: UpdateProfileRequest = {
      firstName: this.profileForm.value.firstName?.trim(),
      lastName: this.profileForm.value.lastName?.trim(),
      telephone: this.profileForm.value.telephone?.trim() || undefined,
      address: this.profileForm.value.address?.trim() || undefined,
      bio: this.profileForm.value.bio?.trim() || undefined
    };

    this.profileUpdate.emit(updateRequest);

    // Reset submitting state after emission (parent will handle loading state)
    setTimeout(() => {
      this.isSubmitting = false;
    }, 500);
  }

  /**
   * Resets form to initial user values.
   */
  onCancel(): void {
    this.initializeForm();
  }

  /**
   * Gets error message for firstName field.
   */
  get firstNameError(): string {
    const control = this.profileForm.get('firstName');
    if (control?.hasError('required')) return 'Le prénom est requis';
    if (control?.hasError('minlength')) return 'Le prénom doit contenir au moins 2 caractères';
    if (control?.hasError('maxlength')) return 'Le prénom ne peut pas dépasser 50 caractères';
    return '';
  }

  /**
   * Gets error message for lastName field.
   */
  get lastNameError(): string {
    const control = this.profileForm.get('lastName');
    if (control?.hasError('required')) return 'Le nom est requis';
    if (control?.hasError('minlength')) return 'Le nom doit contenir au moins 2 caractères';
    if (control?.hasError('maxlength')) return 'Le nom ne peut pas dépasser 50 caractères';
    return '';
  }

  /**
   * Gets error message for telephone field.
   */
  get telephoneError(): string {
    const control = this.profileForm.get('telephone');
    if (control?.hasError('pattern')) return 'Format de téléphone invalide (ex: 06 12 34 56 78)';
    return '';
  }

  /**
   * Gets error message for address field.
   */
  get addressError(): string {
    const control = this.profileForm.get('address');
    if (control?.hasError('maxlength')) return 'L\'adresse ne peut pas dépasser 200 caractères';
    return '';
  }

  /**
   * Gets error message for bio field.
   */
  get bioError(): string {
    const control = this.profileForm.get('bio');
    if (control?.hasError('maxlength')) return 'La biographie ne peut pas dépasser 500 caractères';
    return '';
  }

  /**
   * Gets character count for bio field.
   */
  get bioCharCount(): number {
    return this.profileForm.get('bio')?.value?.length || 0;
  }
}
