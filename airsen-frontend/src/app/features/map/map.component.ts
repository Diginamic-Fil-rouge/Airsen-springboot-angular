import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService } from '@/auth/services/auth.service';
import { AuthUser } from '@/auth/models/auth.model';
import * as L from 'leaflet';
import { GeographicService } from './services/geographic.service';
import { Commune } from './models/commune.model';
import { WeatherService } from './services/weather.service';
import { Weather } from './models/weather.model';
import { AirQualityService } from './services/air-quality.service';
import { AirQuality } from './models/airQuality.model';

@Component({
  standalone: false,
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private geographicService = inject(GeographicService);
  private weatherService = inject(WeatherService);
  private airQualityService = inject(AirQualityService);

  private router = inject(Router);

  currentUser: AuthUser | null = null;
  isLoading = true;
  private destroy$ = new Subject<void>();

  communeClicked: Commune | null = null;
  airQualityClicked: AirQuality | null = null;
  weatherClicked: Weather | null = null;
  communes = new Observable<Commune[]>();
  weatherDatas: Observable<Weather>[] | null = null;
  airQualityDatas: Observable<AirQuality>[] | null = null;
  isLoadingDatas = false;

  dataErrors: string[] | null = null;

  ngOnInit(): void {
    this.loadUserData();
    if (!this.currentUser){
      this.router.navigate(['/auth/login']);
    }
    this.loadMapDatas();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadMapDatas(){
    this.communes = this.geographicService.getCommunesWithCoordinates();

    this.communes.forEach(communes => {
      communes.forEach(commune =>
        this.weatherService.getCurrentWeather(commune.inseeCode).subscribe({ next: (data) => {
          this.weatherDatas?.push(data);
        },
        error: (error) => {
          console.error('Error loading weather data:', error);
        }
      })
      )
    });

    this.communes.forEach(communes => {
      communes.forEach(commune =>
        this.airQualityService.getAirQuality(commune.inseeCode).subscribe({ next: (data) => {
          this.airQualityDatas?.push(data);
        },
        error: (error) => {
          console.error('Error loading air quality data:', error);
        }
      })
      )
    });
  }

  clickEvent(commune: Commune) {
    if (this.communeClicked && this.communeClicked.inseeCode === commune.inseeCode) {
      return;
    }
    
    this.dataErrors = null;
    this.isLoadingDatas = true;

    if (this.communeClicked) {
      this.communeClicked = null;
      this.weatherClicked = null;
      this.airQualityClicked = null;
    }

    let weatherLoaded = false;
    let airQualityLoaded = false;

    this.communeClicked = commune;

    this.weatherDatas?.forEach(datas => {
      console.log("weather : ", datas);
      datas.forEach(data => {
        if (data.communeName === commune.name) {
          this.weatherClicked = data;
          weatherLoaded = true;
        }
      })
    });

    this.airQualityDatas?.forEach(datas => {
      console.log("air quality : ", datas);
      datas.forEach(data => {
        if (data.communeInseeCode === commune.inseeCode) {
          this.airQualityClicked = data;
          airQualityLoaded = true;
        }
      })
    });


    this.isLoadingDatas = false;
    
  }

  /**
   * Load current user data from AuthService
   */
  private loadUserData(): void {
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          this.currentUser = user;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading user data:', error);
          this.isLoading = false;
        }
      });
  }

  /**
   * Navigate to specified route
   */
  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  /**
   * Handle user logout
   */
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
