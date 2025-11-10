import { Component, EventEmitter, OnDestroy, OnInit, Output, inject } from "@angular/core";
import { Subject, Subscription, of } from "rxjs";
import { debounceTime, distinctUntilChanged, map, switchMap, catchError, finalize } from "rxjs/operators";
import { Commune } from "@/shared/models/commune.model";
import { GeographicService } from "../../services/geographic.service";

/**
 * Map Search Bar Component
 *
 * Provides autocomplete search functionality for communes with:
 * - RxJS debouncing (300ms) to reduce API calls
 * - Loading state with spinner
 * - Error handling with user-friendly messages
 * - Keyboard navigation (handled by mat-autocomplete)
 * - Clear button with proper state reset
 */
@Component({
  standalone: false,
  selector: "app-map-search-bar",
  templateUrl: "./map-search-bar.component.html",
})
export class MapSearchBarComponent implements OnInit, OnDestroy {
  private geographicService = inject(GeographicService);

  // Two-way bound from template
  searchQuery = "";

  // Emits when a user selects a search result
  @Output() searchResultSelected = new EventEmitter<Commune>();

  // Internal state for autocomplete list
  filteredCommunes: Commune[] = [];

  // Loading and error states for UX
  isSearching = false;
  searchError: string | null = null;

  // Debounced input stream
  private input$ = new Subject<string>();
  private sub?: Subscription;

  readonly minChars = 2;

  ngOnInit(): void {
    // Set up debounced input handling with GeographicService integration
    // Flow: user types → debounce 300ms → distinctUntilChanged → switchMap to API call
    this.sub = this.input$
      .pipe(
        map((v) => (v ?? "").trim()),
        distinctUntilChanged(),
        debounceTime(300),
        switchMap((query) => {
          // Clear error state on new search
          this.searchError = null;

          // Return empty array for queries shorter than minChars
          if (!query || query.length < this.minChars) {
            this.filteredCommunes = [];
            return of([]);
          }

          // Set loading state before API call
          this.isSearching = true;

          // Call GeographicService with error handling and loading state cleanup
          return this.geographicService.searchCommunes(query).pipe(
            catchError((error) => {
              console.error("Search communes error:", error);
              // Set user-friendly error message
              this.searchError = "Service temporairement indisponible";
              return of([]); // Return empty array to prevent UI crashes
            }),
            finalize(() => {
              // Always reset loading state when search completes (success or error)
              this.isSearching = false;
            })
          );
        })
      )
      .subscribe({
        next: (communes) => {
          this.filteredCommunes = communes;
          console.log(`Found ${communes.length} communes for search query`);
        },
        error: (error) => {
          // This should rarely be hit due to inner catchError, but provides final safety
          console.error("Unexpected search error:", error);
          this.filteredCommunes = [];
          this.searchError = "Service temporairement indisponible";
          this.isSearching = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  /**
   * Called on (ngModelChange) from template
   * Triggers the debounced search flow
   */
  onInputChanged(value: string): void {
    this.input$.next(value);
  }

  /**
   * Display function for mat-autocomplete
   * Returns commune name for display in input field after selection
   */
  displayCommune = (commune?: Commune): string => (commune ? commune.name : "");

  /**
   * Triggered by mat-option selection
   * Emits the selected commune to parent component
   * Keyboard navigation (arrow keys, Enter) is handled automatically by mat-autocomplete
   */
  onOptionSelected(commune: Commune): void {
    if (commune) {
      this.searchResultSelected.emit(commune);
      // Clear search after selection
      this.clear();
    }
  }

  /**
   * Clear input, results, and error state
   * Called when user clicks clear button or after successful selection
   */
  clear(): void {
    this.searchQuery = "";
    this.filteredCommunes = [];
    this.searchError = null;
    this.isSearching = false;
  }

  /**
   * Check if search query is valid (minimum 2 characters)
   */
  get isQueryValid(): boolean {
    return this.searchQuery.trim().length >= this.minChars;
  }

  /**
   * Check if we should show "no results" message
   */
  get showNoResults(): boolean {
    return this.isQueryValid && !this.isSearching && this.filteredCommunes.length === 0 && !this.searchError;
  }
}

