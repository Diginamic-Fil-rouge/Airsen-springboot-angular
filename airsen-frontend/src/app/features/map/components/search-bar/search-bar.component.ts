import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { Observable, of } from "rxjs";
import { catchError, debounceTime, distinctUntilChanged, startWith, switchMap, tap } from "rxjs/operators";
import { CommuneWithAirQuality } from "@/shared/models/commune.model";
import { CommuneDataService } from "@/core/services/commune-data.service";

@Component({
  standalone: false,
  selector: "app-search-bar",
  templateUrl: "./search-bar.component.html",
  styleUrls: ["./search-bar.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchBarComponent implements OnInit {
  @Input() placeholder = "";
  @Input() minSearchLength = 2;
  @Input() maxResults = 10;

  @Output() communeSelected = new EventEmitter<CommuneWithAirQuality>();
  @Output() searchCleared = new EventEmitter<void>();

  searchControl = new FormControl<string>("");
  filteredCommunes$!: Observable<CommuneWithAirQuality[]>;
  showResults = false;

  isLoading = false;
  hasTyped = false;

  constructor(private communeDataService: CommuneDataService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.setupSearchStream();
  }

  private setupSearchStream(): void {
    console.log('[SearchBar] Setting up search stream');

    this.filteredCommunes$ = this.searchControl.valueChanges.pipe(
      startWith(""),
      debounceTime(300),
      distinctUntilChanged(),
      tap((value) => {
        const query = value || "";
        console.log('[SearchBar] Search value changed:', query);
        this.hasTyped = query.length > 0;
        this.showResults = query.length >= this.minSearchLength;

        console.log('[SearchBar] hasTyped:', this.hasTyped, 'showResults:', this.showResults);

        if (query.length < this.minSearchLength) {
          this.isLoading = false;
          this.markForCheck();
        }
      }),
      switchMap((value) => {
        const query = value || "";

        if (!query || query.length < this.minSearchLength) {
          console.log('[SearchBar] Query too short, returning empty array');
          return of([]);
        }

        console.log('[SearchBar] Searching for:', query);
        this.isLoading = true;
        this.markForCheck();

        return this.communeDataService.searchCommunesWithAirQuality(query, this.maxResults).pipe(
          tap((results) => {
            console.log('[SearchBar] Search results:', results.length, 'communes');
            this.isLoading = false;
            this.markForCheck();
          }),
          catchError((error) => {
            console.error('[SearchBar] Search error:', error);
            this.isLoading = false;
            this.markForCheck();
            return of([]);
          })
        );
      })
    );
  }

  onCommuneSelected(commune: CommuneWithAirQuality): void {
    this.communeSelected.emit(commune);
    this.clearSearch();
  }

  clearSearch(): void {
    this.searchControl.setValue("");
    this.hasTyped = false;
    this.showResults = false;
    this.searchCleared.emit();
    this.markForCheck();
  }

  private markForCheck(): void {
    this.cdr.markForCheck();
  }
}
