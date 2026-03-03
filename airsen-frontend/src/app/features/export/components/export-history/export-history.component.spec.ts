import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';

import { ExportHistoryComponent } from './export-history.component';
import { ExportDataService } from '@/core/services/export-data.service';
import { ExportRecord, ExportFormat, ExportType } from '@/shared/models/export.model';

describe('ExportHistoryComponent', () => {
  let component: ExportHistoryComponent;
  let fixture: ComponentFixture<ExportHistoryComponent>;
  let mockExportDataService: jasmine.SpyObj<ExportDataService>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;

  const mockExportRecords: ExportRecord[] = [
    {
      id: '1',
      userId: 1,
      exportType: ExportType.WEATHER,
      format: ExportFormat.CSV,
      locationName: 'Paris',
      inseeCode: '75056',
      fileSize: 2048,
      createdAt: new Date()
    },
    {
      id: '2',
      userId: 1,
      exportType: ExportType.AIR_QUALITY,
      format: ExportFormat.PDF,
      locationName: 'Lyon',
      inseeCode: '69123',
      fileSize: 512,
      createdAt: new Date(Date.now() - 3600000)
    }
  ];

  beforeEach(async () => {
    mockExportDataService = jasmine.createSpyObj('ExportDataService', ['getExportHistory']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ExportHistoryComponent],
      imports: [MatIconModule, MatButtonModule, MatCardModule, MatProgressSpinnerModule],
      providers: [
        { provide: ExportDataService, useValue: mockExportDataService },
        { provide: MatSnackBar, useValue: mockSnackBar }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(ExportHistoryComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load export history on init', () => {
    mockExportDataService.getExportHistory.and.returnValue(of(mockExportRecords));

    fixture.detectChanges();

    expect(mockExportDataService.getExportHistory).toHaveBeenCalled();
    expect(component.exportHistory()).toEqual(mockExportRecords);
    expect(component.isLoading()).toBe(false);
  });

  it('should handle empty export history', () => {
    mockExportDataService.getExportHistory.and.returnValue(of([]));

    fixture.detectChanges();

    expect(component.exportHistory()).toEqual([]);
  });

  it('should handle export history load error', () => {
    const error = new Error('Failed to load history');
    mockExportDataService.getExportHistory.and.returnValue(throwError(() => error));

    fixture.detectChanges();

    expect(component.error()).toBeTruthy();
    expect(component.isLoading()).toBe(false);
  });

  it('should format file sizes correctly', () => {
    expect(component.formatFileSize(500)).toBe('500 B');
    expect(component.formatFileSize(1500)).toBe('1.5 KB');
    expect(component.formatFileSize(1500000)).toBe('1.4 MB');
  });

  it('should return correct format icon', () => {
    expect(component.getFormatIcon('CSV')).toBe('table_chart');
    expect(component.getFormatIcon('PDF')).toBe('picture_as_pdf');
    expect(component.getFormatIcon('UNKNOWN')).toBe('file_download');
  });

  it('should return correct format color class', () => {
    expect(component.getFormatColorClass('CSV')).toBe('format-csv');
    expect(component.getFormatColorClass('PDF')).toBe('format-pdf');
    expect(component.getFormatColorClass('UNKNOWN')).toBe('format-default');
  });

  it('should format relative time correctly', () => {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 3600000);
    const oneDayAgo = new Date(now.getTime() - 86400000);

    expect(component.formatRelativeTime(now.toISOString())).toBe('À l\'instant');
    expect(component.formatRelativeTime(oneHourAgo.toISOString())).toContain('heure');
    expect(component.formatRelativeTime(oneDayAgo.toISOString())).toContain('jour');
  });

  it('should refresh history', () => {
    mockExportDataService.getExportHistory.and.returnValue(of(mockExportRecords));

    component.refreshHistory();

    expect(mockExportDataService.getExportHistory).toHaveBeenCalled();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'Export history refreshed',
      'Fermer',
      jasmine.any(Object)
    );
  });

  it('should clear history', () => {
    component.exportHistory.set(mockExportRecords);

    spyOn(localStorage, 'removeItem');
    component.clearHistory();

    expect(localStorage.removeItem).toHaveBeenCalledWith('airsen_export_history');
    expect(component.exportHistory()).toEqual([]);
  });

  it('should trackByRecordId for ngFor optimization', () => {
    const record = mockExportRecords[0];
    const trackId = component.trackByRecordId(0, record);

    expect(trackId).toBe(record.id);
  });
});
