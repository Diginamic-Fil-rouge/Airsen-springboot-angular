import { NgModule } from '@angular/core';

// Angular Material Modules
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatBadgeModule } from '@angular/material/badge';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';

/**
 * MaterialModule - Centralized Angular Material module imports
 *
 * This module consolidates all Angular Material module imports in one place
 * to avoid repetition across feature modules. 
 */
const MATERIAL_MODULES = [
  MatToolbarModule,
  MatButtonModule,
  MatCardModule,
  MatInputModule,
  MatFormFieldModule,
  MatSelectModule,
  MatIconModule,
  MatMenuModule,
  MatSidenavModule,
  MatListModule,
  MatGridListModule,
  MatChipsModule,
  MatProgressSpinnerModule,
  MatSnackBarModule,
  MatDialogModule,
  MatTabsModule,
  MatTableModule,
  MatPaginatorModule,
  MatSortModule,
  MatDatepickerModule,
  MatNativeDateModule,
  MatSlideToggleModule,
  MatBadgeModule,
  MatCheckboxModule,
  MatTooltipModule,
  MatDividerModule
];

@NgModule({
  imports: MATERIAL_MODULES,
  exports: MATERIAL_MODULES
})
export class MaterialModule { }
