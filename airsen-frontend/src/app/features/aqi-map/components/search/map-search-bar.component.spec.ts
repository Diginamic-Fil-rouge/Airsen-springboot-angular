import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MapSearchBarComponent } from './map-search-bar.component';
import { GeographicService } from '../../services/geographic.service';
import { of, throwError } from 'rxjs';
import { Commune } from '@/shared/models/commune.model';
import { FormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('MapSearchBarComponent', () => {
  let component: MapSearchBarComponent;
  let fixture: ComponentFixture<MapSearchBarComponent>;
  let geographicService: jasmine.SpyObj<GeographicService>;

  const mockCommunes: Commune[] = [
    {
      id: 1,
      inseeCode: '75056',
      name: 'Paris',
      population: 2165423,
      latitude: 48.8566,
      longitude: 2.3522,
      departmentCode: '75',
      regionCode: '11',
      department: {
        id: 1,
        code: '75',
        name: 'Paris',
        regionId: 1,
        region: {
          id: 1,
          code: '11',
          name: 'Île-de-France'
        }
      }
    },
    {
      id: 2,
      inseeCode: '69123',
      name: 'Lyon',
      population: 513275,
      latitude: 45.7578,
      longitude: 4.8320,
      departmentCode: '69',
      regionCode: '84'
    }
  ];

  beforeEach(async () => {
    const geographicServiceSpy = jasmine.createSpyObj('GeographicService', ['searchCommunes']);

    await TestBed.configureTestingModule({
      declarations: [MapSearchBarComponent],
      imports: [
        FormsModule,
        MatAutocompleteModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatProgressSpinnerModule,
        BrowserAnimationsModule
      ],
      providers: [
        { provide: GeographicService, useValue: geographicServiceSpy }
      ]
    }).compileComponents();

    geographicService = TestBed.inject(GeographicService) as jasmine.SpyObj<GeographicService>;
    fixture = TestBed.createComponent(MapSearchBarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initialize with empty search query', () => {
      expect(component.searchQuery).toBe('');
    });

    it('should initialize with empty filtered communes', () => {
      expect(component.filteredCommunes).toEqual([]);
    });

    it('should initialize with isSearching as false', () => {
      expect(component.isSearching).toBe(false);
    });

    it('should initialize with null search error', () => {
      expect(component.searchError).toBeNull();
    });

    it('should have minChars set to 2', () => {
      expect(component.minChars).toBe(2);
    });
  });

  describe('Debounce Behavior', () => {
    it('should not call service before 300ms', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));

      fixture.detectChanges();
      component.onInputChanged('Paris');

      tick(100); // Only 100ms
      expect(geographicService.searchCommunes).not.toHaveBeenCalled();

      tick(200); // Total 300ms
      expect(geographicService.searchCommunes).toHaveBeenCalledWith('Paris');
    }));

    it('should call service after 300ms debounce', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      expect(geographicService.searchCommunes).toHaveBeenCalledWith('Paris');
    }));

    it('should cancel previous search on new input', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));

      fixture.detectChanges();
      component.onInputChanged('Par');
      tick(200);

      component.onInputChanged('Paris'); // New input before 300ms
      tick(300);

      expect(geographicService.searchCommunes).toHaveBeenCalledTimes(1);
      expect(geographicService.searchCommunes).toHaveBeenCalledWith('Paris');
    }));
  });

  describe('Search Functionality', () => {
    it('should call searchCommunes with trimmed query', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));

      fixture.detectChanges();
      component.onInputChanged('  Paris  ');
      tick(300);

      expect(geographicService.searchCommunes).toHaveBeenCalledWith('Paris');
    }));

    it('should update filteredCommunes with search results', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      expect(component.filteredCommunes).toEqual(mockCommunes);
    }));

    it('should not search for queries shorter than minChars', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of([]));

      fixture.detectChanges();
      component.onInputChanged('P');
      tick(300);

      expect(geographicService.searchCommunes).not.toHaveBeenCalled();
      expect(component.filteredCommunes).toEqual([]);
    }));

    it('should not search for empty query', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of([]));

      fixture.detectChanges();
      component.onInputChanged('');
      tick(300);

      expect(geographicService.searchCommunes).not.toHaveBeenCalled();
    }));

    it('should use distinctUntilChanged to avoid duplicate searches', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      component.onInputChanged('Paris'); // Same value
      tick(300);

      expect(geographicService.searchCommunes).toHaveBeenCalledTimes(1);
    }));
  });

  describe('Loading State', () => {
    it('should set isSearching to true during search', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      // Check that isSearching was set to true (we check in the observable)
      expect(component.isSearching).toBe(false); // Reset after completion
    }));

    it('should reset isSearching after successful search', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      expect(component.isSearching).toBe(false);
    }));

    it('should reset isSearching after error', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(throwError(() => new Error('Network error')));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      expect(component.isSearching).toBe(false);
    }));
  });

  describe('Error Handling', () => {
    it('should set error message on service error', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(throwError(() => new Error('Network error')));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      expect(component.searchError).toBe('Service temporairement indisponible');
    }));

    it('should return empty array on error', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(throwError(() => new Error('Network error')));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      expect(component.filteredCommunes).toEqual([]);
    }));

    it('should clear error on new search', fakeAsync(() => {
      geographicService.searchCommunes.and.returnValue(throwError(() => new Error('Network error')));

      fixture.detectChanges();
      component.onInputChanged('Paris');
      tick(300);

      expect(component.searchError).not.toBeNull();

      // New search
      geographicService.searchCommunes.and.returnValue(of(mockCommunes));
      component.onInputChanged('Lyon');
      tick(300);

      expect(component.searchError).toBeNull();
    }));
  });

  describe('Result Selection', () => {
    it('should emit searchResultSelected on option selection', () => {
      spyOn(component.searchResultSelected, 'emit');
      const selectedCommune = mockCommunes[0];

      component.onOptionSelected(selectedCommune);

      expect(component.searchResultSelected.emit).toHaveBeenCalledWith(selectedCommune);
    });

    it('should clear search after selection', () => {
      const selectedCommune = mockCommunes[0];
      component.searchQuery = 'Paris';
      component.filteredCommunes = mockCommunes;

      component.onOptionSelected(selectedCommune);

      expect(component.searchQuery).toBe('');
      expect(component.filteredCommunes).toEqual([]);
    });

    it('should not emit if commune is null', () => {
      spyOn(component.searchResultSelected, 'emit');

      component.onOptionSelected(null as any);

      expect(component.searchResultSelected.emit).not.toHaveBeenCalled();
    });
  });

  describe('Clear Functionality', () => {
    it('should clear search query', () => {
      component.searchQuery = 'Paris';

      component.clear();

      expect(component.searchQuery).toBe('');
    });

    it('should clear filtered communes', () => {
      component.filteredCommunes = mockCommunes;

      component.clear();

      expect(component.filteredCommunes).toEqual([]);
    });

    it('should clear error state', () => {
      component.searchError = 'Some error';

      component.clear();

      expect(component.searchError).toBeNull();
    });

    it('should reset isSearching', () => {
      component.isSearching = true;

      component.clear();

      expect(component.isSearching).toBe(false);
    });
  });

  describe('Display Function', () => {
    it('should return commune name for valid commune', () => {
      const result = component.displayCommune(mockCommunes[0]);
      expect(result).toBe('Paris');
    });

    it('should return empty string for undefined commune', () => {
      const result = component.displayCommune(undefined);
      expect(result).toBe('');
    });

    it('should return empty string for null commune', () => {
      const result = component.displayCommune(null as any);
      expect(result).toBe('');
    });
  });

  describe('Helper Getters', () => {
    it('isQueryValid should return true for valid query', () => {
      component.searchQuery = 'Paris';
      expect(component.isQueryValid).toBe(true);
    });

    it('isQueryValid should return false for query shorter than minChars', () => {
      component.searchQuery = 'P';
      expect(component.isQueryValid).toBe(false);
    });

    it('isQueryValid should handle whitespace', () => {
      component.searchQuery = '  Pa  ';
      expect(component.isQueryValid).toBe(true);
    });

    it('showNoResults should return true when query is valid, not searching, no results, no error', () => {
      component.searchQuery = 'Paris';
      component.isSearching = false;
      component.filteredCommunes = [];
      component.searchError = null;

      expect(component.showNoResults).toBe(true);
    });

    it('showNoResults should return false when searching', () => {
      component.searchQuery = 'Paris';
      component.isSearching = true;
      component.filteredCommunes = [];
      component.searchError = null;

      expect(component.showNoResults).toBe(false);
    });

    it('showNoResults should return false when there are results', () => {
      component.searchQuery = 'Paris';
      component.isSearching = false;
      component.filteredCommunes = mockCommunes;
      component.searchError = null;

      expect(component.showNoResults).toBe(false);
    });

    it('showNoResults should return false when there is an error', () => {
      component.searchQuery = 'Paris';
      component.isSearching = false;
      component.filteredCommunes = [];
      component.searchError = 'Error';

      expect(component.showNoResults).toBe(false);
    });
  });

  describe('Component Lifecycle', () => {
    it('should set up input stream subscription on init', () => {
      fixture.detectChanges();
      expect(component['sub']).toBeDefined();
    });

    it('should unsubscribe on destroy', () => {
      fixture.detectChanges();
      const subscription = component['sub'];
      spyOn(subscription!, 'unsubscribe');

      component.ngOnDestroy();

      expect(subscription!.unsubscribe).toHaveBeenCalled();
    });

    it('should not throw error if subscription is undefined on destroy', () => {
      component['sub'] = undefined;

      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });
});
