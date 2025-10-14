import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors, HTTP_INTERCEPTORS } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

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

// Third-party modules
import { NgxPaginationModule } from 'ngx-pagination';

// App Components
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';

// Auth Components
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';

// Feature Components
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { HomeComponent } from './features/home/home.component';
import { NotFoundComponent } from './features/not-found/not-found.component';
import { CarteComponent } from './features/carte/carte.component';
import { ForumComponent } from './features/forum/forum.component';

// Layout Components
import { HeaderComponent } from './layouts/components/header/header.component';
import { FooterComponent } from './layouts/components/footer/footer.component';
import { SidebarComponent } from './layouts/components/sidebar/sidebar.component';

// Shared Components
import { BreadcrumbComponent } from './shared/components/breadcrumb/breadcrumb.component';

// Page Components
// import { HomeComponent } from './components/pages/home/home.component';
// import { MapComponent } from './components/pages/map/map.component';
// import { ProfileComponent } from './components/pages/profile/profile.component';
// import { FavoritesComponent } from './components/pages/favorites/favorites.component';
// import { HistoryComponent } from './components/pages/history/history.component';
// import { ForumComponent } from './components/pages/forum/forum.component';
// import { NotificationsComponent } from './components/pages/notifications/notifications.component';

// Shared Components
// import { AirQualityCardComponent } from './components/shared/air-quality-card/air-quality-card.component';
// import { WeatherCardComponent } from './components/shared/weather-card/weather-card.component';
// import { ChartComponent } from './components/shared/chart/chart.component';
// import { LoadingSpinnerComponent } from './components/shared/loading-spinner/loading-spinner.component';
// import { SearchBarComponent } from './components/shared/search-bar/search-bar.component';

// Services & Interceptors
import { AuthInterceptor, authInterceptorFn } from './core/interceptors/auth.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    // Layout Components
    HeaderComponent,
    FooterComponent,
    SidebarComponent,
    // Shared Components
    BreadcrumbComponent,
    // Auth Components
    LoginComponent,
    RegisterComponent,
    // Feature Components
    DashboardComponent,
    HomeComponent,
    NotFoundComponent,
    CarteComponent,
    ForumComponent
    // Page Components
    // HomeComponent,
    // MapComponent,
    // ProfileComponent,
    // FavoritesComponent,
    // HistoryComponent,
    // ForumComponent,
    // NotificationsComponent,
    // Shared Components
    // AirQualityCardComponent,
    // WeatherCardComponent,
    // ChartComponent,
    // LoadingSpinnerComponent,
    // SearchBarComponent
  ],
  imports: [
    BrowserModule,
    ReactiveFormsModule,
    FormsModule,
    AppRoutingModule,
    // Angular Material Modules
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
    MatDividerModule,
    // Third-party modules
    NgxPaginationModule
  ],
  providers: [
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptorFn]))
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }