import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';

import { CsvExportPanelComponent } from './csv-export-panel.component';
import { ExportDataService } from '@/services/export-data.service';
import { UserFavoriteResponse } from '@/shared/models/favorite.model';
import { ExportRecord, ExportType, ExportFormat } from '@/shared/models/export.model';

describe('CsvExportPanelComponent', () => {
  let component: CsvExportPanelComponent;
  let fixture: ComponentFixture<CsvExportPanelComponent>;
  let mockExportDataService: jasmine.SpyObj<ExportDataService>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockCdr: jasmine.SpyObj<ChangeDetectorRef>;

  const mockCommune: UserFavoriteResponse = {
    communeInseeCode: '75056',
    communeName: 'Paris 16e Arrondissement',
    departmentName: 'Paris',
    regionName: 'Île-de-France',
    addedAt: '2024-01-01T00:00:00Z'
  };

  const mockExportRecord: ExportRecord = {
    id: '123',
    userId: 1,
    exportType: ExportType.WEATHER,
    format: ExportFormat.CSV,
    locationName: 'Paris 16e',
    inseeCode: '75056',
    fileSize: 1024,
    createdAt: new Date()
  };

  beforeEach(async () => {
    mockExportDataService = jasmine.createSpyObj('ExportDataService', ['exportAsCSV']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    mockCdr = jasmine.createSpyObj('ChangeDetectorRef', ['markForCheck']);

    await TestBed.configureTestingModule({
      declarations: [CsvExportPanelComponent],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: ExportDataService, useValue: mockExportDataService },
        { provide: MatSnackBar, useValue: mockSnackBar },
        { provide: ChangeDetectorRef, useValue: mockCdr }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(CsvExportPanelComponent);
    component = fixture.componentInstance;
    component.selectedCommune = mockCommune;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    component.ngOnInit();

    expect(component.exportForm.get('startDate')?.value).toBeDefined();
    expect(component.exportForm.get('endDate')?.value).toBeDefined();

    const startDate = component.exportForm.get('startDate')?.value as Date;
    const endDate = component.exportForm.get('endDate')?.value as Date;

    expect(startDate).toBeInstanceOf(Date);
    expect(endDate).toBeInstanceOf(Date);
    expect(startDate.getTime()).toBeLessThan(endDate.getTime());
  });

  it('should validate form correctly', () => {
    component.ngOnInit();

    // Valid form
    expect(component.exportForm.valid).toBe(true);

    // Invalid date range (start > end)
    const today = new Date();
    const tomorrow = new Date(today.getTime() + 24 * 60 * 60 * 1000);

    component.exportForm.patchValue({
      startDate: tomorrow,
      endDate: today
    });

    expect(component.exportForm.hasError('dateRangeInvalid')).toBe(true);

    // Range too large (> 90 days)
    const ninetyOneDaysAgo = new Date(today.getTime() - 91 * 24 * 60 * 60 * 1000);

    component.exportForm.patchValue({
      startDate: ninetyOneDaysAgo,
      endDate: today
    });

    expect(component.exportForm.hasError('dateRangeTooLarge')).toBe(true);
  });

  it('should export CSV successfully', () => {
    mockExportDataService.exportAsCSV.and.returnValue(of(mockExportRecord));
    component.ngOnInit();

    const startDate = new Date('2024-01-01');
    const endDate = new Date('2024-01-07');

    component.exportForm.patchValue({
      startDate,
      endDate
    });

    spyOn(component.exportComplete, 'emit');

    component.onExport();

    expect(mockExportDataService.exportAsCSV).toHaveBeenCalledWith(
      '75056',
      '2024-01-01',
      '2024-01-07'
    );
    expect(component.exportComplete.emit).toHaveBeenCalled();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      jasmine.stringMatching(/Exporté CSV pour Paris 16e Arrondissement/),
      'Fermer',
      { duration: 4000 }
    );
  });

  it('should handle export error', () => {
    const error = new Error('Export failed');
    mockExportDataService.exportAsCSV.and.returnValue(throwError(error));
    component.ngOnInit();

    component.onExport();

    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'Erreur : Export failed',
      'Fermer',
      { duration: 5000 }
    );
    expect(component.isExporting).toBe(false);
  });

  it('should show validation errors on invalid form', () => {
    component.ngOnInit();

    // Make form invalid
    component.exportForm.patchValue({
      startDate: null,
      endDate: null
    });

    component.onExport();

    expect(mockExportDataService.exportAsCSV).not.toHaveBeenCalled();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'Veuillez corriger les erreurs dans le formulaire',
      'Fermer',
      { duration: 4000 }
    );
  });

  it('should emit panel closed event', () => {
    spyOn(component.panelClosed, 'emit');

    component.onClose();

    expect(component.panelClosed.emit).toHaveBeenCalled();
  });

  it('should set loading state during export', () => {
    mockExportDataService.exportAsCSV.and.returnValue(of(mockExportRecord));
    component.ngOnInit();

    expect(component.isExporting).toBe(false);

    component.onExport();

    expect(component.isExporting).toBe(false); // Should be reset after success
  });

  it('should load user preferences on init', () => {
    const preferences = {
      startDateOffset: -7,
      endDateOffset: 0
    };
    spyOn(localStorage, 'getItem').and.returnValue(JSON.stringify(preferences));

    component.ngOnInit();

    const startDate = component.exportForm.get('startDate')?.value as Date;
    const endDate = component.exportForm.get('endDate')?.value as Date;

    expect(startDate).toBeDefined();
    expect(endDate).toBeDefined();
  });

  it('should save user preferences after successful export', () => {
    spyOn(localStorage, 'setItem');
    mockExportDataService.exportAsCSV.and.returnValue(of(mockExportRecord));
    component.ngOnInit();

    component.onExport();

    expect(localStorage.setItem).toHaveBeenCalledWith(
      'airsen_csv_export_preferences',
      jasmine.any(String)
    );
  });

  it('should format dates correctly', () => {
    const testDate = new Date('2024-01-15');
    const formatted = (component as any).formatDate(testDate);

    expect(formatted).toBe('2024-01-15');
  });

  it('should format file sizes correctly', () => {
    expect((component as any).formatFileSize(500)).toBe('500 B');
    expect((component as any).formatFileSize(1500)).toBe('1.5 KB');
    expect((component as any).formatFileSize(1500000)).toBe('1.4 MB');
  });
});