import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * InputDialogComponent
 *
 * A reusable input dialog component for collecting text input.
 * Displays a modal with title, message, input field, and submit/cancel buttons.
 */
@Component({
  selector: 'app-input-dialog',
  templateUrl: './input-dialog.component.html',
  styleUrls: ['./input-dialog.component.scss']
})
export class InputDialogComponent {
  @Input() isOpen = false;
  @Input() title = 'Input Required';
  @Input() message = 'Please enter a value:';
  @Input() placeholder = '';
  @Input() inputValue = '';
  @Input() submitText = 'Submit';
  @Input() cancelText = 'Cancel';
  @Input() required = true;

  @Output() submitted = new EventEmitter<string>();
  @Output() cancelled = new EventEmitter<void>();

  onSubmit(): void {
    if (this.required && !this.inputValue.trim()) {
      return;
    }
    this.submitted.emit(this.inputValue);
    this.close();
  }

  onCancel(): void {
    this.cancelled.emit();
    this.close();
  }

  close(): void {
    this.isOpen = false;
    this.inputValue = '';
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.onCancel();
    }
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.onSubmit();
    } else if (event.key === 'Escape') {
      this.onCancel();
    }
  }
}
