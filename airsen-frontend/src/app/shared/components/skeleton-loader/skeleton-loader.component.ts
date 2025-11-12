/**
 * Skeleton Loader Component
 *
 * Displays a skeleton/placeholder while content is loading.
 * Supports multiple variants: line, paragraph, card, avatar, badge.
 *
 * Usage:
 * <app-skeleton-loader [type]="'paragraph'" [lines]="3"></app-skeleton-loader>
 * <app-skeleton-loader type="card" [height]="300"></app-skeleton-loader>
 */

import {
  Component,
  Input,
  ChangeDetectionStrategy,
} from '@angular/core';

type SkeletonType = 'line' | 'paragraph' | 'card' | 'avatar' | 'badge' | 'button' | 'circle';

@Component({
  selector: 'app-skeleton-loader',
  standalone: false,
  templateUrl: './skeleton-loader.component.html',
  styleUrls: ['./skeleton-loader.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SkeletonLoaderComponent {
  /**
   * Type of skeleton to display
   * - line: Single line of text
   * - paragraph: Multiple lines (use with lines input)
   * - card: Full card container
   * - avatar: Circular avatar
   * - badge: Small badge/pill
   * - button: Button-sized skeleton
   * - circle: Perfect circle (for icons)
   */
  @Input() type: SkeletonType = 'paragraph';

  /**
   * Number of lines for paragraph type
   * Default: 3
   */
  @Input() lines = 3;

  /**
   * Height of skeleton element (in pixels or with unit)
   * Example: '200', '200px', '100%'
   */
  @Input() height: string | number = 'auto';

  /**
   * Width of skeleton element
   * Example: '100%', '200px', '50'
   */
  @Input() width: string | number = '100%';

  /**
   * Animation type: 'pulse' or 'shimmer'
   * Default: 'shimmer'
   */
  @Input() animation: 'pulse' | 'shimmer' = 'shimmer';

  /**
   * Border radius
   * Default: '8px' (for cards), '50%' (for avatars)
   */
  @Input() borderRadius = '8px';

  /**
   * Gap between skeleton lines (for paragraph type)
   * Default: '8px'
   */
  @Input() lineGap = '8px';

  /**
   * CSS class for additional styling
   */
  @Input() customClass = '';

  /**
   * Computed properties based on type
   */
  get computedHeight(): string {
    if (this.height !== 'auto') {
      return typeof this.height === 'number' ? `${this.height}px` : this.height;
    }

    switch (this.type) {
      case 'avatar':
        return '48px';
      case 'circle':
        return '48px';
      case 'badge':
        return '24px';
      case 'button':
        return '40px';
      case 'card':
        return '300px';
      case 'line':
        return '16px';
      case 'paragraph':
        return '16px';
      default:
        return '16px';
    }
  }

  get computedWidth(): string {
    if (this.width !== '100%') {
      return typeof this.width === 'number' ? `${this.width}px` : this.width;
    }

    switch (this.type) {
      case 'avatar':
        return '48px';
      case 'circle':
        return '48px';
      case 'badge':
        return '80px';
      case 'button':
        return '120px';
      case 'line':
        return '100%';
      case 'paragraph':
        return '100%';
      case 'card':
        return '100%';
      default:
        return '100%';
    }
  }

  get computedBorderRadius(): string {
    if (this.type === 'avatar' || this.type === 'circle') {
      return '50%';
    }
    if (this.type === 'badge') {
      return '12px';
    }
    return this.borderRadius;
  }

  /**
   * Get array of lines for paragraph type
   * Used in *ngFor to render multiple skeleton lines
   */
  get lineArray(): number[] {
    return Array.from({ length: this.lines }, (_, i) => i);
  }

  /**
   * Determine if we should show multiple lines
   */
  get showMultipleLines(): boolean {
    return this.type === 'paragraph' && this.lines > 1;
  }

  /**
   * Get CSS class string for skeleton element
   */
  get skeletonClass(): string {
    const classes = [
      'skeleton-loader',
      `skeleton-${this.type}`,
      `skeleton-${this.animation}`,
    ];

    if (this.customClass) {
      classes.push(this.customClass);
    }

    return classes.join(' ');
  }
}
