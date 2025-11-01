/**
 * AIRSEN Animation Library
 *
 * Comprehensive animation definitions using Angular's animation API.
 * Includes entrance animations, exit animations, state transitions,
 * and staggered animations for list items.
 *
 * Usage in components:
 * - Import: import { fadeInAnimation, slideInAnimation, ... } from '@/shared/animations/animations';
 * - Apply: @Component({ animations: [fadeInAnimation] })
 * - Trigger: [@fadeIn]="triggerState"
 *
 * Animation Categories:
 * - Entrance: fadeIn, slideIn, slideUp, zoomIn
 * - Exit: fadeOut, slideOut
 * - Cards: cardExpand, cardCollapse, cardFlip
 * - Lists: listStagger, itemStagger
 * - Loading: pulseAnimation, skeletonPulse
 */

import {
  trigger,
  state,
  style,
  animate,
  transition,
  keyframes,
  query,
  stagger,
  animateChild,
  group,
} from '@angular/animations';

// ============================================================================
// Entrance Animations
// ============================================================================

/**
 * Fade in animation with opacity transition
 * Duration: 500ms | Easing: easeInOut
 *
 * Usage:
 * @Component({
 *   animations: [fadeInAnimation]
 * })
 * export class MyComponent {
 *   @Component({
 *     template: '<div [@fadeIn]="isVisible">Content</div>'
 *   })
 * }
 */
export const fadeInAnimation = trigger('fadeIn', [
  transition(':enter', [
    style({ opacity: 0 }),
    animate('500ms 100ms ease-in-out', style({ opacity: 1 })),
  ]),
  transition(':leave', [
    animate('300ms ease-in-out', style({ opacity: 0 })),
  ]),
]);

/**
 * Fade in and scale animation
 * Creates a subtle zoom effect during fade in
 * Duration: 600ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@fadeInScale]="isVisible">Content</div>'
 * })
 */
export const fadeInScaleAnimation = trigger('fadeInScale', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(0.95)' }),
    animate('600ms 100ms ease-out', style({ opacity: 1, transform: 'scale(1)' })),
  ]),
  transition(':leave', [
    animate('300ms ease-in', style({ opacity: 0, transform: 'scale(0.95)' })),
  ]),
]);

/**
 * Slide in from left animation
 * Duration: 400ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@slideInLeft]="isVisible">Content</div>'
 * })
 */
export const slideInLeftAnimation = trigger('slideInLeft', [
  transition(':enter', [
    style({ transform: 'translateX(-100%)', opacity: 0 }),
    animate('400ms 100ms ease-out', style({ transform: 'translateX(0)', opacity: 1 })),
  ]),
  transition(':leave', [
    animate('300ms ease-in', style({ transform: 'translateX(-100%)', opacity: 0 })),
  ]),
]);

/**
 * Slide in from right animation
 * Duration: 400ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@slideInRight]="isVisible">Content</div>'
 * })
 */
export const slideInRightAnimation = trigger('slideInRight', [
  transition(':enter', [
    style({ transform: 'translateX(100%)', opacity: 0 }),
    animate('400ms 100ms ease-out', style({ transform: 'translateX(0)', opacity: 1 })),
  ]),
  transition(':leave', [
    animate('300ms ease-in', style({ transform: 'translateX(100%)', opacity: 0 })),
  ]),
]);

/**
 * Slide up from bottom animation (for modals, drawers)
 * Duration: 400ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@slideUp]="isVisible">Modal</div>'
 * })
 */
export const slideUpAnimation = trigger('slideUp', [
  transition(':enter', [
    style({ transform: 'translateY(100%)', opacity: 0 }),
    animate('400ms 100ms ease-out', style({ transform: 'translateY(0)', opacity: 1 })),
  ]),
  transition(':leave', [
    animate('300ms ease-in', style({ transform: 'translateY(100%)', opacity: 0 })),
  ]),
]);

/**
 * Slide down animation
 * Duration: 400ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@slideDown]="isVisible">Dropdown</div>'
 * })
 */
export const slideDownAnimation = trigger('slideDown', [
  transition(':enter', [
    style({ transform: 'translateY(-100%)', opacity: 0 }),
    animate('400ms 100ms ease-out', style({ transform: 'translateY(0)', opacity: 1 })),
  ]),
  transition(':leave', [
    animate('300ms ease-in', style({ transform: 'translateY(-100%)', opacity: 0 })),
  ]),
]);

/**
 * Zoom in animation (scale from small to normal)
 * Duration: 500ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@zoomIn]="isVisible">Zoomed content</div>'
 * })
 */
export const zoomInAnimation = trigger('zoomIn', [
  transition(':enter', [
    style({ transform: 'scale(0.8)', opacity: 0 }),
    animate('500ms 100ms cubic-bezier(0.34, 1.56, 0.64, 1)',
      style({ transform: 'scale(1)', opacity: 1 })
    ),
  ]),
  transition(':leave', [
    animate('300ms ease-in', style({ transform: 'scale(0.8)', opacity: 0 })),
  ]),
]);

/**
 * Bounce in animation (scales with bounce effect)
 * Duration: 600ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@bounceIn]="isVisible">Bouncy content</div>'
 * })
 */
export const bounceInAnimation = trigger('bounceIn', [
  transition(':enter', [
    style({ transform: 'scale(0)', opacity: 0 }),
    animate('600ms 100ms cubic-bezier(0.68, -0.55, 0.265, 1.55)',
      style({ transform: 'scale(1)', opacity: 1 })
    ),
  ]),
]);

// ============================================================================
// Card & Container Animations
// ============================================================================

/**
 * Card expand animation (grows from center)
 * Duration: 400ms
 *
 * Usage in dashboard:
 * @Component({
 *   template: '<div [@cardExpand]="cardState">AQI Card</div>'
 * })
 */
export const cardExpandAnimation = trigger('cardExpand', [
  state('collapsed', style({
    transform: 'scale(0.9)',
    opacity: 0.6,
  })),
  state('expanded', style({
    transform: 'scale(1)',
    opacity: 1,
  })),
  transition('collapsed <=> expanded', [
    animate('400ms 100ms ease-out'),
  ]),
  transition(':enter', [
    style({ transform: 'scale(0.9)', opacity: 0.6 }),
    animate('400ms 100ms ease-out', style({ transform: 'scale(1)', opacity: 1 })),
  ]),
]);

/**
 * Card flip animation (3D rotation)
 * Duration: 600ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@cardFlip]="isFlipped">Content</div>'
 * })
 */
export const cardFlipAnimation = trigger('cardFlip', [
  state('normal', style({
    transform: 'rotateY(0deg)',
  })),
  state('flipped', style({
    transform: 'rotateY(180deg)',
  })),
  transition('normal <=> flipped', [
    animate('600ms ease-in-out'),
  ]),
]);

/**
 * Card slide animation (swipe effect)
 * Duration: 300ms
 *
 * Usage:
 * @Component({
 *   template: '<div [@cardSlide]="slideDirection">Sliding card</div>'
 * })
 */
export const cardSlideAnimation = trigger('cardSlide', [
  state('left', style({
    transform: 'translateX(-100%)',
    opacity: 0,
  })),
  state('center', style({
    transform: 'translateX(0)',
    opacity: 1,
  })),
  state('right', style({
    transform: 'translateX(100%)',
    opacity: 0,
  })),
  transition('* => *', [
    animate('300ms ease-in-out'),
  ]),
]);

/**
 * Accordion expand/collapse animation
 * Duration: 300ms
 *
 * Usage in filters or settings:
 * @Component({
 *   template: '<div [@expandCollapse]="isExpanded">Content</div>'
 * })
 */
export const expandCollapseAnimation = trigger('expandCollapse', [
  state('collapsed', style({
    height: '0px',
    overflow: 'hidden',
    opacity: 0,
  })),
  state('expanded', style({
    height: '*',
    overflow: 'visible',
    opacity: 1,
  })),
  transition('collapsed <=> expanded', [
    animate('300ms ease-in-out'),
  ]),
]);

// ============================================================================
// List & Item Animations
// ============================================================================

/**
 * List stagger animation (animates items with delay)
 * Perfect for dashboard widgets, alert lists
 *
 * Usage:
 * @Component({
 *   animations: [listStaggerAnimation]
 *   template: `
 *     <div [@listStagger]>
 *       <div *ngFor="let item of items">{{ item }}</div>
 *     </div>
 *   `
 * })
 */
export const listStaggerAnimation = trigger('listStagger', [
  transition('* <=> *', [
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(15px)' }),
      stagger('50ms', [
        animate('400ms 100ms ease-out',
          style({ opacity: 1, transform: 'translateY(0)' })
        ),
      ]),
    ], { optional: true }),
  ]),
]);

/**
 * Item fade in stagger (for quick list animations)
 * Duration: 300ms with 40ms stagger
 *
 * Usage:
 * @Component({
 *   animations: [itemStaggerAnimation]
 * })
 */
export const itemStaggerAnimation = trigger('itemStagger', [
  transition('* <=> *', [
    query(':enter', [
      style({ opacity: 0 }),
      stagger('40ms', [
        animate('300ms ease-out', style({ opacity: 1 })),
      ]),
    ], { optional: true }),
  ]),
]);

/**
 * Staggered scale animation for grid layouts
 * Duration: 400ms with 60ms stagger
 *
 * Usage:
 * @Component({
 *   animations: [gridStaggerAnimation]
 * })
 */
export const gridStaggerAnimation = trigger('gridStagger', [
  transition('* <=> *', [
    query(':enter', [
      style({ opacity: 0, transform: 'scale(0.9)' }),
      stagger('60ms', [
        animate('400ms cubic-bezier(0.34, 1.56, 0.64, 1)',
          style({ opacity: 1, transform: 'scale(1)' })
        ),
      ]),
    ], { optional: true }),
  ]),
]);

// ============================================================================
// Loading & Skeleton Animations
// ============================================================================

/**
 * Pulse animation (for loading states, skeleton loaders)
 * Infinite loop with opacity change
 *
 * Usage:
 * @Component({
 *   animations: [pulseAnimation]
 *   template: '<div [@pulse]>Loading...</div>'
 * })
 */
export const pulseAnimation = trigger('pulse', [
  state('active', style({ opacity: 1 })),
  transition('* => active', [
    animate('2s ease-in-out',
      keyframes([
        style({ opacity: 1, offset: 0 }),
        style({ opacity: 0.5, offset: 0.5 }),
        style({ opacity: 1, offset: 1 }),
      ])
    ),
  ]),
]);

/**
 * Skeleton loader pulse animation
 * Shimmer effect from left to right
 *
 * Usage:
 * @Component({
 *   animations: [skeletonPulseAnimation]
 *   template: '<div [@skeletonPulse]>Skeleton</div>'
 * })
 */
export const skeletonPulseAnimation = trigger('skeletonPulse', [
  state('loading', style({
    backgroundPosition: '200% 0',
  })),
  transition('* => loading', [
    animate('2s linear infinite',
      style({
        backgroundPosition: '-200% 0',
      })
    ),
  ]),
]);

/**
 * Shimmer loading animation (gradient shimmer)
 * Creates a smooth left-to-right shimmer effect
 *
 * Usage in template:
 * <div [@shimmer]="isLoading ? 'loading' : 'idle'"></div>
 */
export const shimmerAnimation = trigger('shimmer', [
  state('loading', style({
    backgroundPosition: '200% center',
  })),
  state('idle', style({
    backgroundPosition: '0% center',
  })),
  transition('loading <=> idle', [
    animate('1.5s ease-in-out'),
  ]),
  transition('* => loading', [
    animate('1.5s linear infinite',
      keyframes([
        style({ backgroundPosition: '200% center', offset: 0 }),
        style({ backgroundPosition: '-200% center', offset: 1 }),
      ])
    ),
  ]),
]);

// ============================================================================
// Complex Combined Animations
// ============================================================================

/**
 * Page transition animation (staggered content entrance)
 * Animates backdrop + title + content
 *
 * Usage:
 * @Component({
 *   animations: [pageTransitionAnimation]
 *   template: `
 *     <div [@pageTransition]>
 *       <h1>Title</h1>
 *       <div>Content</div>
 *     </div>
 *   `
 * })
 */
export const pageTransitionAnimation = trigger('pageTransition', [
  transition(':enter', [
    query('h1, .content', [
      style({ opacity: 0, transform: 'translateY(20px)' }),
      stagger('100ms', [
        animate('500ms ease-out',
          style({ opacity: 1, transform: 'translateY(0)' })
        ),
      ]),
    ], { optional: true }),
  ]),
]);

/**
 * Dashboard widget animation (scale + fade + stagger)
 * Perfect for dashboard with multiple widgets
 *
 * Usage:
 * @Component({
 *   animations: [dashboardWidgetAnimation]
 *   template: `
 *     <div [@dashboardWidget]>
 *       <mat-card>Widget 1</mat-card>
 *       <mat-card>Widget 2</mat-card>
 *     </div>
 *   `
 * })
 */
export const dashboardWidgetAnimation = trigger('dashboardWidget', [
  transition(':enter', [
    query('mat-card, [class*="card"]', [
      style({ opacity: 0, transform: 'scale(0.9)' }),
      stagger('80ms', [
        animate('500ms 100ms cubic-bezier(0.34, 1.56, 0.64, 1)',
          style({ opacity: 1, transform: 'scale(1)' })
        ),
      ]),
    ], { optional: true }),
  ]),
]);

/**
 * Modal/Dialog animation (backdrop + content)
 * Dimmed background + scaled content
 *
 * Usage:
 * @Component({
 *   animations: [modalAnimation]
 *   template: `
 *     <div class="modal-backdrop" [@modalAnimation]>
 *       <div class="modal-content">...</div>
 *     </div>
 *   `
 * })
 */
export const modalAnimation = trigger('modalAnimation', [
  transition(':enter', [
    style({ opacity: 0 }),
    group([
      animate('300ms ease-out', style({ opacity: 1 })),
      query('.modal-content', [
        style({ transform: 'scale(0.9)', opacity: 0 }),
        animate('400ms 100ms cubic-bezier(0.34, 1.56, 0.64, 1)',
          style({ transform: 'scale(1)', opacity: 1 })
        ),
      ], { optional: true }),
    ]),
  ]),
  transition(':leave', [
    group([
      animate('300ms ease-in', style({ opacity: 0 })),
      query('.modal-content', [
        animate('300ms ease-in',
          style({ transform: 'scale(0.9)', opacity: 0 })
        ),
      ], { optional: true }),
    ]),
  ]),
]);

// ============================================================================
// AQI-Specific Animations
// ============================================================================

/**
 * AQI value change animation (number update)
 * Pops when value changes
 *
 * Usage:
 * @Component({
 *   animations: [aqiValueChangeAnimation]
 *   template: '<div [@aqiValueChange]="aqiValue">{{ aqiValue }}</div>'
 * })
 */
export const aqiValueChangeAnimation = trigger('aqiValueChange', [
  transition(':increment, :decrement', [
    animate('300ms cubic-bezier(0.34, 1.56, 0.64, 1)',
      keyframes([
        style({ transform: 'scale(1)', offset: 0 }),
        style({ transform: 'scale(1.15)', offset: 0.5 }),
        style({ transform: 'scale(1)', offset: 1 }),
      ])
    ),
  ]),
]);

/**
 * AQI status color change animation
 * Fades out current color, fades in new color
 *
 * Usage:
 * @Component({
 *   animations: [aqiColorChangeAnimation]
 *   template: '<div [@aqiColorChange]="aqiLevel">{{ aqiLabel }}</div>'
 * })
 */
export const aqiColorChangeAnimation = trigger('aqiColorChange', [
  state('good', style({ color: '#2d6a4f', borderColor: '#52b788' })),
  state('moderate', style({ color: '#f59e0b', borderColor: '#f59e0b' })),
  state('unhealthy-sensitive', style({ color: '#f97316', borderColor: '#fb923c' })),
  state('unhealthy', style({ color: '#dc2626', borderColor: '#f87171' })),
  state('very-unhealthy', style({ color: '#7c2d12', borderColor: '#d97706' })),
  state('hazardous', style({ color: '#7e0023', borderColor: '#a91155' })),
  transition('* <=> *', [
    animate('400ms ease-in-out'),
  ]),
]);

// ============================================================================
// Utility Animations
// ============================================================================

/**
 * Rotate animation (infinite)
 * Usage:
 * @Component({
 *   animations: [rotateAnimation]
 *   template: '<mat-spinner [@rotate]="isLoading"></mat-spinner>'
 * })
 */
export const rotateAnimation = trigger('rotate', [
  state('active', style({ transform: 'rotate(360deg)' })),
  transition('* => active', [
    animate('1s linear', style({ transform: 'rotate(360deg)' })),
  ]),
]);

/**
 * Shake animation (error feedback)
 * Shakes left and right for 400ms
 *
 * Usage:
 * @Component({
 *   animations: [shakeAnimation]
 *   template: '<input [@shake]="hasError">'
 * })
 */
export const shakeAnimation = trigger('shake', [
  transition('false => true', [
    animate('400ms',
      keyframes([
        style({ transform: 'translateX(0)', offset: 0 }),
        style({ transform: 'translateX(-10px)', offset: 0.25 }),
        style({ transform: 'translateX(10px)', offset: 0.5 }),
        style({ transform: 'translateX(-10px)', offset: 0.75 }),
        style({ transform: 'translateX(0)', offset: 1 }),
      ])
    ),
  ]),
]);

/**
 * Glow animation (subtle pulse glow)
 * Increases and decreases box-shadow for attention
 *
 * Usage:
 * @Component({
 *   animations: [glowAnimation]
 *   template: '<div [@glow]="needsAttention">Important</div>'
 * })
 */
export const glowAnimation = trigger('glow', [
  state('active', style({ boxShadow: '0 0 20px rgba(76, 175, 80, 0.8)' })),
  transition('* => active', [
    animate('1.5s ease-in-out',
      keyframes([
        style({ boxShadow: '0 0 5px rgba(76, 175, 80, 0.3)', offset: 0 }),
        style({ boxShadow: '0 0 20px rgba(76, 175, 80, 0.8)', offset: 0.5 }),
        style({ boxShadow: '0 0 5px rgba(76, 175, 80, 0.3)', offset: 1 }),
      ])
    ),
  ]),
]);

// ============================================================================
// Animation Configuration Objects
// ============================================================================

/**
 * Timing presets for consistent animation speeds
 * Use these for custom animations
 */
export const AnimationTimings = {
  // Duration
  FAST: '200ms',
  NORMAL: '400ms',
  SLOW: '600ms',

  // Delay
  NO_DELAY: '0ms',
  SHORT_DELAY: '50ms',
  MEDIUM_DELAY: '100ms',
  LONG_DELAY: '200ms',

  // Easing
  EASE_IN: 'ease-in',
  EASE_OUT: 'ease-out',
  EASE_IN_OUT: 'ease-in-out',
  EASE_BOUNCE: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
  EASE_SHARP: 'cubic-bezier(0.4, 0, 0.2, 1)',
};

/**
 * Stagger timing presets for list animations
 */
export const StaggerTimings = {
  TIGHT: '30ms',       // For compact lists
  NORMAL: '50ms',      // Default stagger
  LOOSE: '80ms',       // For spacious layouts
  VERY_LOOSE: '120ms', // For card grids
};
