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
import { FavoritesService } from '../favorites/services/favorites.service';
import { NgClass } from '@angular/common';

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
  private favoriteService = inject(FavoritesService);
  private router = inject(Router);

  currentUser: AuthUser | null = null;
  isLoading = true;
  private destroy$ = new Subject<void>();

  communeClicked: Commune | null = null;
  communeClickedIsFavorite: boolean = false;
  communeSearched: Commune | null = null;
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

  /**
   * Updates the search results based on the current search query.
   * Calls the searchCommunes function of the GeographicService with the current search query,
   * and assigns the result to the searchResults observable.
   */
  onSearchInput(){
    this.searchResults = this.geographicService.searchCommunes(this.searchQuery);
  }

  /**
   * Handles the event when a search result is clicked.
   * Resets the search query to the name of the clicked commune,
   * resets the search results to an empty observable, and
   * calls the clickEvent function with the clicked commune and the type "NEW".
   * @param commune The Commune object of the clicked search result.
   */
  onSearchResultClicked(commune: Commune){
    this.searchQuery = '';
    this.searchResults = new Observable<Commune[]>();
    this.communeSearched = commune;
    this.clickEvent(commune, "NEW");
  }

  /**
   * Resets the search results to an empty observable.
   * This is used to clear the search results when the user wants to go back to the original map view.
   */
  closeSearchResults(){
    this.searchResults = new Observable<Commune[]>();
  }

  /**
   * Scrolls to the element with the id "map-datas" when called.
   * Does nothing if the element does not exist.
   */
  goToAnchor(){
    const element = document.getElementById("map-datas");
    if (element) {
      element.scrollIntoView({ behavior: "smooth" });
    }
  }

  /**
   * Fetches weather and air quality data for the given commune,
   * and stores the results in the component's state.
   * Prevents duplicate clicks on the same commune by
   * resetting the state when a new commune is clicked.
   * @param commune The Commune object to fetch data for.
   * @param type The type of data to fetch, either "NEW" or "LATEST".
   * @returns A promise that resolves when the data has been fetched.
   */
  async clickEvent(commune: Commune, type: string): Promise<void> {
    // Prevent duplicate clicks on same commune
    if (this.communeClicked && this.communeClicked.inseeCode === commune.inseeCode) {
      return;
    }

    // Reset state for new commune
    this.communeClicked = commune;
    this.communeClickedIsFavorite = false;
    this.weatherClicked = null;
    this.airQualityClicked = null;
    this.dataErrors = null;
    this.isLoadingDatas = true;

    console.log("Fetching data for commune:", commune.name, "INSEE Code:", commune.inseeCode);


      // Fetch both weather and air quality data in parallel for better performance
      const [weather, airQuality] = await Promise.all([
        firstValueFrom(this.weatherService.getCurrentWeather(commune.inseeCode)).catch(() => null),
        type === "LATEST" ? firstValueFrom(this.airQualityService.getAirLatestQuality(commune.inseeCode)).catch(() => null) : firstValueFrom(this.airQualityService.getAirQuality(commune.inseeCode)).catch(() => null),
      ]);

      this.favoriteService.checkIfIsFavorite(this.currentUser?.id, commune.inseeCode).subscribe({
        next: (data) => {
          this.communeClickedIsFavorite = data.isFavorited;
        },
        error: (error) => {
          console.error("Error checking if commune is favorite:", error);
        }
      });

      console.log("Weather data received:", weather);
      console.log("Air quality data received:", airQuality);

      this.weatherClicked = weather;
      this.airQualityClicked = airQuality;
    
      this.isLoadingDatas = false;
    
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

  onFavoriteButtonClicked(commune: Commune){
    if (this.communeClickedIsFavorite) {
      this.removeFavorite();
    } else {
      this.addFavorite();
    }
  }

  addFavorite(){
    this.favoriteService.addFavorite(this.currentUser?.id, this.communeClicked?.inseeCode).subscribe({
      next: (data) => {
        this.communeClickedIsFavorite = true;
      },
      error: (error) => {
        console.error("Error adding favorite:", error);
      }
    });
  }

  removeFavorite(){
    this.favoriteService.removeFavorite(this.currentUser?.id, this.communeClicked?.inseeCode).subscribe({
      next: () => {
        this.communeClickedIsFavorite = false;
      },
      error: (error) => {
        console.error("Error removing favorite:", error);
      }
    });
  }

  /**
   * Handle user logout
   */
  logout(): void {
    this.authService.logout();
    this.router.navigate(["/auth/login"]);
  }
}
