import { Component, OnInit, OnDestroy, inject } from "@angular/core";
import { Observable, firstValueFrom } from "rxjs";
import { Router } from "@angular/router";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { AuthService } from "@/auth/services/auth.service";
import { AuthUser } from "@/auth/models/auth.model";
import * as L from "leaflet";
import { GeographicService } from "./services/geographic.service";
import { Commune } from "./models/commune.model";
import { WeatherService } from "./services/weather.service";
import { Weather } from "./models/weather.model";
import { AirQualityService } from "./services/air-quality.service";
import { AirQuality } from "./models/airQuality.model";

@Component({
  standalone: false,
  selector: "app-map",
  templateUrl: "./map.component.html",
  styleUrls: ["./map.component.scss"],
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
  airQualityClicked: any | null = null;
  weatherClicked: Weather | null = null;
  communes = new Observable<Commune[]>();
  isLoadingDatas = false;

  dataErrors: string[] | null = null;

  searchQuery: string = "";
  searchResults =  new Observable<Commune[]>();

  ngOnInit(): void {
    this.loadUserData();
    if (!this.currentUser) {
      this.router.navigate(["/auth/login"]);
    }
    this.communes = this.geographicService.getCommunesWithCoordinatesAndMinPop();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchInput(){
    this.searchResults = this.geographicService.searchCommunes(this.searchQuery);
  }

  onSearchResultClicked(commune: Commune){
    this.searchQuery = commune.name;
    this.searchResults = new Observable<Commune[]>();
    this.clickEvent(commune, "NEW");
  }

  closeSearchResults(){
    this.searchResults = new Observable<Commune[]>();
  }

  async clickEvent(commune: Commune, type: string): Promise<void> {
    // Prevent duplicate clicks on same commune
    if (this.communeClicked && this.communeClicked.inseeCode === commune.inseeCode) {
      return;
    }

    // Reset state for new commune
    this.communeClicked = commune;
    this.weatherClicked = null;
    this.airQualityClicked = null;
    this.dataErrors = null;
    this.isLoadingDatas = true;

    console.log("Fetching data for commune:", commune.name, "INSEE Code:", commune.inseeCode);

    try {
      // Fetch both weather and air quality data in parallel for better performance
      const [weather, airQuality] = await Promise.all([
        firstValueFrom(this.weatherService.getCurrentWeather(commune.inseeCode)),
        type === "LATEST" ? firstValueFrom(this.airQualityService.getAirLatestQuality(commune.inseeCode)) : firstValueFrom(this.airQualityService.getAirQuality(commune.inseeCode)),
      ]);

      console.log("Weather data received:", weather);
      console.log("Air quality data received:", airQuality);

      this.weatherClicked = weather;
      this.airQualityClicked = airQuality;
    } catch (error: any) {
      console.error("Detailed error loading commune data:", error);
      console.error("Error status:", error?.status);
      console.error("Error message:", error?.message);
      console.error("Error body:", error?.error);

      // Handle specific error cases
      if (error?.status === 404) {
        this.dataErrors = ["No data available for this commune."];
      } else {
        const errorMsg = error?.error?.message || error?.message || "Unknown error";
        this.dataErrors = [`Failed to load data: ${errorMsg}`];
      }
    } finally {
      this.isLoadingDatas = false;
    }
  }

  /**
   * Load current user data from AuthService
   */
  private loadUserData(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (user) => {
        this.currentUser = user;
        this.isLoading = false;
      },
      error: (error) => {
        console.error("Error loading user data:", error);
        this.isLoading = false;
      },
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
    this.router.navigate(["/auth/login"]);
  }
}
