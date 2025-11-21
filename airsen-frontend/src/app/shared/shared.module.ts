import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { RouterModule } from "@angular/router";

// Material Module
import { MaterialModule } from "./material/material.module";

// Components
import { LoaderComponent } from "./components/loader/loader.component";
import { BackButtonComponent } from "./components/backButton/back-button.component";
import { BreadcrumbComponent } from "./components/breadcrumb/breadcrumb.component";
import { SkeletonLoaderComponent } from "./components/skeleton-loader/skeleton-loader.component";
import { SidebarComponent } from "./components/sidebar/sidebar.component";
import { FavoriteButtonComponent } from "./components/favorite-button/favorite-button.component";
import { QuickExportButtonComponent } from "./components/quick-export-button/quick-export-button.component";
import { AdvancedExportPanelComponent } from "./components/advanced-export-panel/advanced-export-panel.component";
import { ConfirmDialogComponent } from "./components/confirm-dialog/confirm-dialog.component";
import { InputDialogComponent } from "./components/input-dialog/input-dialog.component";
import { ToastComponent } from "./components/toast/toast.component";

// Pipes
import { AqiColorPipe } from "./pipes/aqi-color.pipe";

/**
 * SharedModule - Reusable components, pipes, and modules for AIRSEN application
 *
 * This module contains UI components, utility pipes, and common modules that are
 * used across multiple feature modules. It follows Angular best practices by
 * centralizing shared functionality and reducing code duplication.
 *
 * Components:
 * - LoaderComponent: Loading spinner for async operations
 * - BackButtonComponent: Navigation back button
 * - BreadcrumbComponent: Breadcrumb navigation trail
 * - SkeletonLoaderComponent: Skeleton loading placeholders
 * - SidebarComponent: Collapsible navigation sidebar
 *
 * Pipes:
 * - AqiLabelPipe: Convert AQI number to label (Good, Moderate, Unhealthy, etc.)
 * - AqiColorPipe: Convert AQI number to EPA standard color
 *
 * Modules:
 * - MaterialModule: All Angular Material modules
 * - FormsModule & ReactiveFormsModule: Form handling
 * - RouterModule: Routing directives (routerLink, etc.)
 *
 * Usage:
 * Import SharedModule in EVERY feature module:
 *
 * @NgModule({
 *   imports: [
 *     SharedModule  // All shared components, pipes, Material available
 *   ],
 *   declarations: [MyFeatureComponent]
 * })
 * export class MyFeatureModule { }
 *
 * Important:
 * - DO NOT provide services here (use CoreModule for services)
 * - DO export everything (components, pipes, modules)
 * - CAN be imported in multiple feature modules
 */
@NgModule({
  declarations: [
    // Components
    LoaderComponent,
    BackButtonComponent,
    BreadcrumbComponent,
    SkeletonLoaderComponent,
    SidebarComponent,
    FavoriteButtonComponent,
    QuickExportButtonComponent,
    AdvancedExportPanelComponent,
    ConfirmDialogComponent,
    InputDialogComponent,
    ToastComponent,
    // Pipes
    AqiColorPipe,
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, MaterialModule],
  exports: [
    // Modules - re-export for convenience
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    MaterialModule,
    // Components
    LoaderComponent,
    BackButtonComponent,
    BreadcrumbComponent,
    SkeletonLoaderComponent,
    SidebarComponent,
    FavoriteButtonComponent,
    QuickExportButtonComponent,
    AdvancedExportPanelComponent,
    ConfirmDialogComponent,
    InputDialogComponent,
    ToastComponent,
    // Pipes
    AqiColorPipe,
  ],
})
export class SharedModule {}
