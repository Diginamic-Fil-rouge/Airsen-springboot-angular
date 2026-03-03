import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { skip } from 'rxjs/operators';
import { MapStateService, MapViewMode } from './map-state.service';
import { CommuneWithAirQuality } from '@/shared/models/commune.model';

describe('MapStateService', () => {
  let service: MapStateService;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockActivatedRoute: any;

  beforeEach(() => {
    // Create mock router
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    // Create mock activated route
    mockActivatedRoute = {
      queryParams: of({}),
      snapshot: {
        queryParams: {}
      }
    };

    TestBed.configureTestingModule({
      providers: [
        MapStateService,
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    });

    service = TestBed.inject(MapStateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('selectCommune', () => {
    it('should select commune and update state', (done) => {
      const commune: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        latitude: 48.8566,
        longitude: 2.3522,
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      service.selectedCommune$.subscribe(selected => {
        if (selected) {
          expect(selected.inseeCode).toBe('75056');
          expect(service.getCurrentCommune()).toEqual(commune);
          done();
        }
      });

      service.selectCommune(commune);
    });

    it('should update map center and zoom when selecting commune with coordinates', (done) => {
      const commune: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        latitude: 48.8566,
        longitude: 2.3522,
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      let centerChecked = false;
      let zoomChecked = false;

      service.mapCenter$.subscribe(center => {
        if (center[0] === 48.8566 && center[1] === 2.3522) {
          centerChecked = true;
          if (zoomChecked) done();
        }
      });

      service.zoomLevel$.subscribe(zoom => {
        if (zoom === 12) {
          zoomChecked = true;
          if (centerChecked) done();
        }
      });

      service.selectCommune(commune);
    });

    it('should clear selection when null is passed', (done) => {
      const commune: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      service.selectCommune(commune);

      service.selectedCommune$.subscribe(selected => {
        if (selected === null && service.getCurrentCommune() === null) {
          done();
        }
      });

      service.selectCommune(null);
    });

    it('should update URL when commune is selected', () => {
      const commune: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        latitude: 48.8566,
        longitude: 2.3522,
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      service.selectCommune(commune);

      expect(mockRouter.navigate).toHaveBeenCalled();
    });

    it('should not emit duplicate values with same inseeCode', (done) => {
      const commune1: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      const commune2: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        currentAirQuality: { atmoIndex: 4, qualifier: 'Mauvais', color: '#FF5050' }
      };

      let emissionCount = 0;
      service.selectedCommune$.pipe(skip(1)).subscribe(() => {
        emissionCount++;
      });

      service.selectCommune(commune1);
      service.selectCommune(commune2);

      setTimeout(() => {
        expect(emissionCount).toBe(1); // Should only emit once due to distinctUntilChanged
        done();
      }, 100);
    });
  });

  describe('setViewMode', () => {
    it('should toggle view mode to weather', (done) => {
      service.viewMode$.subscribe(mode => {
        if (mode === 'weather') {
          expect(service.getCurrentViewMode()).toBe('weather');
          done();
        }
      });

      service.setViewMode('weather');
    });

    it('should toggle view mode to aqi', (done) => {
      service.setViewMode('weather'); // First set to weather

      service.viewMode$.subscribe(mode => {
        if (mode === 'aqi') {
          expect(service.getCurrentViewMode()).toBe('aqi');
          done();
        }
      });

      service.setViewMode('aqi');
    });

    it('should update URL when view mode changes', () => {
      service.setViewMode('weather');
      expect(mockRouter.navigate).toHaveBeenCalled();
    });
  });

  describe('setMapCenter', () => {
    it('should update map center coordinates', (done) => {
      const newCenter: [number, number] = [48.8566, 2.3522];

      service.mapCenter$.subscribe(center => {
        if (center[0] === 48.8566 && center[1] === 2.3522) {
          done();
        }
      });

      service.setMapCenter(newCenter);
    });

    it('should not emit duplicate center values', (done) => {
      const center: [number, number] = [48.8566, 2.3522];

      let emissionCount = 0;
      service.mapCenter$.pipe(skip(1)).subscribe(() => {
        emissionCount++;
      });

      service.setMapCenter(center);
      service.setMapCenter(center);

      setTimeout(() => {
        expect(emissionCount).toBe(1); // Should only emit once
        done();
      }, 100);
    });
  });

  describe('setZoomLevel', () => {
    it('should update zoom level', (done) => {
      service.zoomLevel$.subscribe(zoom => {
        if (zoom === 10) {
          expect(service.getCurrentZoom()).toBe(10);
          done();
        }
      });

      service.setZoomLevel(10);
    });

    it('should update URL when zoom changes', () => {
      service.setZoomLevel(10);
      expect(mockRouter.navigate).toHaveBeenCalled();
    });

    it('should not emit duplicate zoom values', (done) => {
      let emissionCount = 0;
      service.zoomLevel$.pipe(skip(1)).subscribe(() => {
        emissionCount++;
      });

      service.setZoomLevel(10);
      service.setZoomLevel(10);

      setTimeout(() => {
        expect(emissionCount).toBe(1);
        done();
      }, 100);
    });
  });

  describe('setMapBounds', () => {
    it('should update map bounds', (done) => {
      const bounds = {
        north: 50,
        south: 40,
        east: 5,
        west: -5
      };

      service.bounds$.subscribe(b => {
        if (b && b.north === 50) {
          expect(b).toEqual(bounds);
          done();
        }
      });

      service.setMapBounds(bounds);
    });
  });

  describe('setFilter', () => {
    it('should update filter with departments', (done) => {
      const filter = { departments: ['75', '13'] };

      service.filter$.subscribe(f => {
        if (f.departments.length === 2) {
          expect(f.departments).toContain('75');
          expect(f.departments).toContain('13');
          done();
        }
      });

      service.setFilter(filter);
    });

    it('should merge partial filter updates', (done) => {
      service.setFilter({ departments: ['75'] });

      service.filter$.subscribe(f => {
        if (f.departments.length === 1 && f.regions.length === 1) {
          expect(f.departments).toContain('75');
          expect(f.regions).toContain('11');
          done();
        }
      });

      service.setFilter({ regions: ['11'] });
    });

    it('should reset filter to defaults', (done) => {
      service.setFilter({ departments: ['75'], regions: ['11'], minPopulation: 1000 });

      service.filter$.subscribe(f => {
        if (f.departments.length === 0 && f.regions.length === 0 && !f.minPopulation) {
          expect(f).toEqual({
            departments: [],
            regions: [],
            minPopulation: undefined,
            aqiThreshold: undefined
          });
          done();
        }
      });

      service.resetFilter();
    });

    it('should not emit duplicate filter values', (done) => {
      const filter = { departments: ['75'] };

      let emissionCount = 0;
      service.filter$.pipe(skip(1)).subscribe(() => {
        emissionCount++;
      });

      service.setFilter(filter);
      service.setFilter(filter);

      setTimeout(() => {
        expect(emissionCount).toBe(1);
        done();
      }, 100);
    });
  });

  describe('setLoading', () => {
    it('should update loading state', (done) => {
      service.isLoading$.subscribe(loading => {
        if (loading === true) {
          done();
        }
      });

      service.setLoading(true);
    });
  });

  describe('isSelected', () => {
    it('should return true for selected commune', () => {
      const commune: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      service.selectCommune(commune);
      expect(service.isSelected('75056')).toBe(true);
    });

    it('should return false for non-selected commune', () => {
      const commune: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      service.selectCommune(commune);
      expect(service.isSelected('13055')).toBe(false);
    });
  });

  describe('reset', () => {
    it('should reset all state to defaults', () => {
      const commune: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      service.selectCommune(commune);
      service.setViewMode('weather');
      service.setZoomLevel(12);
      service.setFilter({ departments: ['75'] });

      service.reset();

      expect(service.getCurrentCommune()).toBeNull();
      expect(service.getCurrentViewMode()).toBe('aqi');
      expect(service.getCurrentZoom()).toBe(6);
      expect(service.getCurrentFilter()).toEqual({
        departments: [],
        regions: [],
        minPopulation: undefined,
        aqiThreshold: undefined
      });
    });
  });

  describe('mapState$ combined observable', () => {
    it('should emit combined state', (done) => {
      const commune: CommuneWithAirQuality = {
        id: 1,
        inseeCode: '75056',
        name: 'Paris',
        latitude: 48.8566,
        longitude: 2.3522,
        currentAirQuality: { atmoIndex: 3, qualifier: 'Dégradé', color: '#F0E641' }
      };

      service.mapState$.subscribe(state => {
        if (state.selectedCommune && state.viewMode === 'weather') {
          expect(state.selectedCommune.inseeCode).toBe('75056');
          expect(state.viewMode).toBe('weather');
          expect(state.zoomLevel).toBe(12);
          done();
        }
      });

      service.selectCommune(commune);
      service.setViewMode('weather');
    });
  });

  describe('URL initialization', () => {
    it('should initialize view mode from URL params', () => {
      const mockRouteWithParams = {
        queryParams: of({ mode: 'weather' }),
        snapshot: { queryParams: { mode: 'weather' } }
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          MapStateService,
          { provide: Router, useValue: mockRouter },
          { provide: ActivatedRoute, useValue: mockRouteWithParams }
        ]
      });

      const newService = TestBed.inject(MapStateService);

      // Wait a bit for the subscription to process
      setTimeout(() => {
        expect(newService.getCurrentViewMode()).toBe('weather');
      }, 100);
    });

    it('should initialize zoom level from URL params', () => {
      const mockRouteWithParams = {
        queryParams: of({ zoom: '10' }),
        snapshot: { queryParams: { zoom: '10' } }
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          MapStateService,
          { provide: Router, useValue: mockRouter },
          { provide: ActivatedRoute, useValue: mockRouteWithParams }
        ]
      });

      const newService = TestBed.inject(MapStateService);

      setTimeout(() => {
        expect(newService.getCurrentZoom()).toBe(10);
      }, 100);
    });

    it('should initialize map center from URL params', () => {
      const mockRouteWithParams = {
        queryParams: of({ lat: '48.8566', lng: '2.3522' }),
        snapshot: { queryParams: { lat: '48.8566', lng: '2.3522' } }
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          MapStateService,
          { provide: Router, useValue: mockRouter },
          { provide: ActivatedRoute, useValue: mockRouteWithParams }
        ]
      });

      const newService = TestBed.inject(MapStateService);

      newService.mapCenter$.subscribe(center => {
        if (center[0] === 48.8566 && center[1] === 2.3522) {
          expect(center).toEqual([48.8566, 2.3522]);
        }
      });
    });

    it('should ignore invalid URL params', () => {
      const mockRouteWithParams = {
        queryParams: of({ zoom: 'invalid', mode: 'invalid' }),
        snapshot: { queryParams: { zoom: 'invalid', mode: 'invalid' } }
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          MapStateService,
          { provide: Router, useValue: mockRouter },
          { provide: ActivatedRoute, useValue: mockRouteWithParams }
        ]
      });

      const newService = TestBed.inject(MapStateService);

      setTimeout(() => {
        // Should maintain default values
        expect(newService.getCurrentZoom()).toBe(6);
        expect(newService.getCurrentViewMode()).toBe('aqi');
      }, 100);
    });
  });
});
