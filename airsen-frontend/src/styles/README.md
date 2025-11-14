# Material Design Styles Directory

This directory contains all global Angular Material customizations for the Airsen application.

## File Structure

```
styles/
├── _material-theme.scss       # Material theme configuration (colors, typography)
├── _material-components.scss  # Global Material component customizations
└── README.md                  # This file
```

## Files Description

### `_material-theme.scss`
**Purpose:** Defines the Material Design theme for the entire application.

**Contains:**
- Custom color palettes (primary, accent, warn)
- Typography configuration
- Theme generation using Angular Material's theming system

**When to modify:**
- Changing primary/accent/warn colors
- Updating typography (font families, sizes)
- Adjusting Material density

**DO NOT:**
- Add component-specific styles here
- Add non-Material related styles

---

### `_material-components.scss`
**Purpose:** Global customizations for Angular Material components.

**Contains:**
- Form field styles (mat-form-field)
- Button styles (mat-button, mat-raised-button)
- Checkbox styles (mat-checkbox)
- Progress spinner styles (mat-progress-spinner)
- Dialog styles (mat-dialog)
- Snackbar styles (mat-snack-bar)
- Utility classes

**When to modify:**
- Styling Material components globally
- Adding new Material component customizations
- Creating utility classes for Material components

**DO NOT:**
- Add component-specific business logic styles
- Use `::ng-deep` (styles are already global)
- Mix with non-Material component styles

---

## What NOT to do

### Don't add these styles here:
- Custom component styles (use component SCSS files)
- Page-specific styles (use page component SCSS files)
- Non-Material UI elements
- Business logic related styles

### Don't use `::ng-deep`:
These files are already imported globally in `styles.scss`, so `::ng-deep` is unnecessary and deprecated.

---

## Best Practices

### 1. **Separation of Concerns**
```scss
// GOOD - In _material-components.scss
.mat-mdc-form-field {
  // Global Material customization
}

// BAD - Don't mix with custom components
.my-custom-form {
  // This belongs in component SCSS
}
```

### 2. **No ::ng-deep**
```scss
// GOOD - Direct styling (already global)
.mat-mdc-checkbox {
  .mdc-checkbox__background {
    border-color: #e5e7eb;
  }
}

// BAD - Unnecessary ::ng-deep
::ng-deep .mat-mdc-checkbox {
  // Not needed here
}
```

### 3. **Use Theme Variables**
```scss
// GOOD - Use theme colors
.mat-mdc-raised-button.mat-primary {
  background: linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%);
}

// BAD - Hardcoded unrelated colors
.mat-mdc-raised-button {
  background: #ff0000; // Random color
}
```

---

## How to Use

### Importing in `styles.scss`
```scss
// In src/styles.scss
@import 'styles/material-theme';
@import 'styles/material-components';
```

### Component SCSS Files
**Component files should NOT import these files.**
They are already available globally.

```scss
// GOOD - In component.scss
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  // Component-specific layout
}

// BAD - Don't re-import
@import 'styles/material-components'; // Already global!
```

---

## Common Customizations

### Adding a new Material component style
1. Open `_material-components.scss`
2. Add a new section with comments
3. Follow existing patterns

```scss
// ============================================================================
// New Component Name
// ============================================================================

.mat-mdc-new-component {
  // Your customizations
}
```

### Changing theme colors
1. Open `_material-theme.scss`
2. Modify the palette definitions
3. The changes apply globally

```scss
$airsen-primary-palette: (
  500: #4caf50,  // Change this color
  // ...
);
```

---

## References

- [Angular Material Theming Guide](https://material.angular.io/guide/theming)
- [Angular Material Component Styles](https://material.angular.io/guide/customizing-component-styles)
- [Material Design Color System](https://m3.material.io/styles/color/system/overview)

---

## Maintenance

**Last Updated:** October 2025
**Angular Version:** 17+
**Material Version:** 17+

**Maintainers:** Airsen Development Team
