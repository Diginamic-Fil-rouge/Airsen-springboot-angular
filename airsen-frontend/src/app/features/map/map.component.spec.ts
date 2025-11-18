import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of, BehaviorSubject } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { MapComponent } from './map.component';
import { CommuneDataService } from '@/core/services/commune-data.service';
import { CommuneWithAirQuality } from '@/shared/models/commune.model';

describe('MapComponent', () => {
  let component: MapComponent;
  let fixture: ComponentFixture<MapComponent>;
  let mockCommuneDataService: any;
  let mockBreakpointObserver: any;
  let mockActivatedRoute: any;

  const mockCommunes: CommuneWithAirQuality[] = [
    {
      id: 1,
      inseeCode: '75056',
      name: 'Paris',
      latitude: 48.8566,
      longitude: 2.3522,
      population: 2165423,
      currentAirQuality: {
        atmoIndex: 3,
        qualifier: 'Dégradé',
        color: '#F0E641'
      }
    },
    {
      id: 2,
      inseeCode: '13055',
      name: 'Marseille',
      latitude: 43.2965,
      longitude: 5.3698,
      population: 869815,
      currentAirQuality: {
        atmoIndex: 2,
        qualifier: 'Bon',
        color: '#50F0E6'
      }
    }
  ];

  beforeEach(async () => {
    const loadingSubject = new BehaviorSubject<boolean>(false);
    const errorSubject = new BehaviorSubject<string | null>(null);
    const queryParamsSubject = new BehaviorSubject<any>({});

    mockCommuneDataService = {
      loading$: loadingSubject.asObservable(),
      error$: errorSubject.asObservable(),
      getAllCommunesWithCoordinates: jasmine.createSpy('getAllCommunesWithCoordinates').and.returnValue(of(mockCommunes)),
      getCommuneDetail: jasmine.createSpy('getCommuneDetail').and.returnValue(
        of({
          ...mockCommunes[0],
          pollutants: [
            { name: 'PM10', value: 35, unit: 'µg/m³', code: 'PM10' },
            { name: 'NO2', value: 22, unit: 'µg/m³', code: 'NO2' }
          ]
        })
      ),
      clearError: jasmine.createSpy('clearError')
    };

    mockBreakpointObserver = {
      observe: jasmine.createSpy('observe').and.returnValue(
        of({
          matches: true,
          breakpoints: {
            '(min-width: 1024px)': true,
            '(min-width: 640px) and (max-width: 1023px)': false,
            '(max-width: 639px)': false
          }
        })
      )
    };

    mockActivatedRoute = {
      queryParams: queryParamsSubject.asObservable(),
      snapshot: {
        queryParams: {}
      }
    };

    await TestBed.configureTestingModule({
      declarations: [MapComponent],
      providers: [
        { provide: CommuneDataService, useValue: mockCommuneDataService },
        { provide: BreakpointObserver, useValue: mockBreakpointObserver },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(MapComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load major cities on initialization', () => {
      fixture.detectChanges();

      expect(mockCommuneDataService.getAllCommunesWithCoordinates).toHaveBeenCalledWith(50000);
    });

    it('should setup sidebar mode listener', () => {
      fixture.detectChanges();

      expect(mockBreakpointObserver.observe).toHaveBeenCalled();
    });
  });

  describe('query param handling with combineLatest', () => {
    it('should wait for communes to load before processing query params', fakeAsync(() => {
      const queryParamsSubject = mockActivatedRoute.queryParams as BehaviorSubject<any>;

      // Set query params BEFORE communes are loaded
      queryParamsSubject.next({ commune: '75056', openSidebar: 'true' });

      spyOn(component, 'onCommuneClicked');
      spyOn(component, 'openSidebar');

      // Initialize component (communes not loaded yet)
      fixture.detectChanges();
      tick();

      // Query params should NOT trigger yet (communes not loaded)
      expect(component.onCommuneClicked).not.toHaveBeenCalled();
      expect(component.openSidebar).not.toHaveBeenCalled();

      // Simulate communes loading (this triggers combineLatest)
      component.communes = mockCommunes;
      component['communesLoaded$'].next(true);
      tick();

      // Now query params should be processed
      expect(component.onCommuneClicked).toHaveBeenCalledWith(mockCommunes[0]);
      expect(component.openSidebar).toHaveBeenCalled();
    }));

    it('should select commune when commune query param is provided after communes load', fakeAsync(() => {
      spyOn(component, 'onCommuneClicked');
      spyOn(component, 'openSidebar');

      fixture.detectChanges();

      // Simulate communes loading
      component.communes = mockCommunes;
      component['communesLoaded$'].next(true);
      tick();

      // Emit query params AFTER communes are loaded
      const queryParamsSubject = mockActivatedRoute.queryParams as BehaviorSubject<any>;
      queryParamsSubject.next({ commune: '75056', openSidebar: 'true' });
      tick();

      expect(component.onCommuneClicked).toHaveBeenCalledWith(mockCommunes[0]);
      expect(component.openSidebar).toHaveBeenCalled();
    }));

    it('should not call onCommuneClicked if commune not found in list', fakeAsync(() => {
      spyOn(component, 'onCommuneClicked');

      fixture.detectChanges();

      // Load communes
      component.communes = mockCommunes;
      component['communesLoaded$'].next(true);
      tick();

      // Emit query params with non-existent commune
      const queryParamsSubject = mockActivatedRoute.queryParams as BehaviorSubject<any>;
      queryParamsSubject.next({ commune: 'non-existent-code', openSidebar: 'true' });
      tick();

      expect(component.onCommuneClicked).not.toHaveBeenCalled();
    }));

    it('should open sidebar only when openSidebar=true', fakeAsync(() => {
      spyOn(component, 'onCommuneClicked');
      spyOn(component, 'openSidebar');

      fixture.detectChanges();

      component.communes = mockCommunes;
      component['communesLoaded$'].next(true);
      tick();

      const queryParamsSubject = mockActivatedRoute.queryParams as BehaviorSubject<any>;
      queryParamsSubject.next({ commune: '75056', openSidebar: 'false' });
      tick();

      expect(component.onCommuneClicked).toHaveBeenCalledWith(mockCommunes[0]);
      expect(component.openSidebar).not.toHaveBeenCalled();
    }));

    it('should handle query params without openSidebar param', fakeAsync(() => {
      spyOn(component, 'onCommuneClicked');
      spyOn(component, 'openSidebar');

      fixture.detectChanges();

      component.communes = mockCommunes;
      component['communesLoaded$'].next(true);
      tick();

      const queryParamsSubject = mockActivatedRoute.queryParams as BehaviorSubject<any>;
      queryParamsSubject.next({ commune: '13055' });
      tick();

      expect(component.onCommuneClicked).toHaveBeenCalledWith(mockCommunes[1]);
      expect(component.openSidebar).not.toHaveBeenCalled();
    }));

    it('should do nothing when no commune query param provided', fakeAsync(() => {
      spyOn(component, 'onCommuneClicked');
      spyOn(component, 'openSidebar');

      fixture.detectChanges();

      component.communes = mockCommunes;
      component['communesLoaded$'].next(true);
      tick();

      const queryParamsSubject = mockActivatedRoute.queryParams as BehaviorSubject<any>;
      queryParamsSubject.next({});
      tick();

      expect(component.onCommuneClicked).not.toHaveBeenCalled();
      expect(component.openSidebar).not.toHaveBeenCalled();
    }));

    it('should handle race condition: query params before communes', fakeAsync(() => {
      const queryParamsSubject = mockActivatedRoute.queryParams as BehaviorSubject<any>;

      spyOn(component, 'onCommuneClicked');
      spyOn(component, 'openSidebar');

      // User navigates to /map?commune=75056&openSidebar=true
      queryParamsSubject.next({ commune: '75056', openSidebar: 'true' });

      fixture.detectChanges();
      tick();

      // combineLatest should NOT trigger yet (waiting for communes)
      expect(component.onCommuneClicked).not.toHaveBeenCalled();

      // Simulate async commune loading completing
      component.communes = mockCommunes;
      component['communesLoaded$'].next(true);
      tick();

      // Now combineLatest should trigger
      expect(component.onCommuneClicked).toHaveBeenCalledWith(mockCommunes[0]);
      expect(component.openSidebar).toHaveBeenCalled();
    }));
  });

  describe('onCommuneClicked', () => {
    beforeEach(() => {
      fixture.detectChanges();
      component.communes = mockCommunes;
    });

    it('should fetch commune details and update selectedCommune', (done) => {
      component.onCommuneClicked(mockCommunes[0]);

      expect(mockCommuneDataService.getCommuneDetail).toHaveBeenCalledWith('75056');

      setTimeout(() => {
        expect(component.selectedCommune).toBeTruthy();
        expect(component.selectedCommune?.inseeCode).toBe('75056');
        done();
      }, 100);
    });

    it('should set isProgrammaticZoom flag to prevent progressive loading', () => {
      component.onCommuneClicked(mockCommunes[0]);

      expect(component['isProgrammaticZoom']).toBe(true);
    });

    it('should open sidebar on mobile/tablet mode', (done) => {
      component.sidebarDisplayMode = 'mobile';
      component.isSidebarOpen = false;

      component.onCommuneClicked(mockCommunes[0]);

      setTimeout(() => {
        expect(component.isSidebarOpen).toBe(true);
        done();
      }, 100);
    });

    it('should not auto-open sidebar on desktop mode', (done) => {
      component.sidebarDisplayMode = 'desktop';
      component.isSidebarOpen = false;

      component.onCommuneClicked(mockCommunes[0]);

      setTimeout(() => {
        expect(component.isSidebarOpen).toBe(false);
        done();
      }, 100);
    });
  });

  describe('openSidebar', () => {
    it('should set isSidebarOpen to true', () => {
      component.isSidebarOpen = false;

      component.openSidebar();

      expect(component.isSidebarOpen).toBe(true);
    });
  });

  describe('progressive loading', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should load 20K+ communes at zoom 8', () => {
      component['currentZoom'] = 6;
      component['loadedPopulationThreshold'] = 50000;

      component.onMapZoomChanged(8);

      expect(mockCommuneDataService.getAllCommunesWithCoordinates).toHaveBeenCalledWith(20000);
    });

    it('should load 10K+ communes at zoom 10', () => {
      component['currentZoom'] = 8;
      component['loadedPopulationThreshold'] = 20000;

      component.onMapZoomChanged(10);

      expect(mockCommuneDataService.getAllCommunesWithCoordinates).toHaveBeenCalledWith(10000);
    });

    it('should skip progressive loading on programmatic zoom', () => {
      component['currentZoom'] = 6;
      component['isProgrammaticZoom'] = true;

      component.onMapZoomChanged(12);

      // Should only have initial load call, not progressive load
      expect(mockCommuneDataService.getAllCommunesWithCoordinates).toHaveBeenCalledTimes(1);
    });
  });

  describe('ngOnDestroy', () => {
    it('should complete destroy$ subject', () => {
      fixture.detectChanges();
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
