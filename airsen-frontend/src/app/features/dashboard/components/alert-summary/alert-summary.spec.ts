import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AlertSummaryComponent } from './alert-summary';
import { AlertSummaryItem } from '../../models/alert-summary';
import { By } from '@angular/platform-browser';
import { MatIconModule } from '@angular/material/icon';

describe('AlertSummaryComponent', () => {
  let component: AlertSummaryComponent;
  let fixture: ComponentFixture<AlertSummaryComponent>;

  const mockAlerts: AlertSummaryItem[] = [
    {
      id: 1,
      title: 'High PM2.5 Alert',
      location: 'Paris',
      severity: 'high',
      status: 'pending',
      icon: 'warning',
      timestamp: '2 hours ago'
    },
    {
      id: 2,
      title: 'Ozone Level Warning',
      location: 'Lyon',
      severity: 'medium',
      status: 'sent',
      icon: 'notifications',
      timestamp: '1 day ago'
    },
    {
      id: 3,
      title: 'Air Quality Normal',
      location: 'Marseille',
      severity: 'low',
      status: 'sent',
      icon: 'check_circle',
      timestamp: '3 days ago'
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertSummaryComponent, MatIconModule]
    }).compileComponents();

    fixture = TestBed.createComponent(AlertSummaryComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display alerts when provided', () => {
    component.alerts = mockAlerts;
    fixture.detectChanges();

    const alertElements = fixture.debugElement.queryAll(By.css('.alert-item'));
    expect(alertElements.length).toBe(3);
  });

  it('should emit alertClick event when alert is clicked', () => {
    component.alerts = mockAlerts;
    fixture.detectChanges();

    const spy = spyOn(component.alertClick, 'emit');
    const firstAlert = fixture.debugElement.query(By.css('.alert-item'));
    
    firstAlert.triggerEventHandler('click', null);
    
    expect(spy).toHaveBeenCalledWith(1);
  });

  it('should emit viewAllAlerts event when view all button is clicked', () => {
    component.alerts = mockAlerts;
    fixture.detectChanges();

    const spy = spyOn(component.viewAllAlerts, 'emit');
    const viewAllButton = fixture.debugElement.query(By.css('.link-button'));
    
    viewAllButton.triggerEventHandler('click', null);
    
    expect(spy).toHaveBeenCalled();
  });

  it('should emit filterByFavorites event when favorites button is clicked', () => {
    component.alerts = mockAlerts;
    component.showFavoritesFilter = true;
    fixture.detectChanges();

    const spy = spyOn(component.filterByFavorites, 'emit');
    const favoritesButton = fixture.debugElement.queryAll(By.css('.link-button'))[1];
    
    favoritesButton.triggerEventHandler('click', null);
    
    expect(spy).toHaveBeenCalled();
  });

  it('should apply correct CSS classes based on severity', () => {
    component.alerts = mockAlerts;
    fixture.detectChanges();

    const alertElements = fixture.debugElement.queryAll(By.css('.alert-icon'));
    
    expect(alertElements[0].nativeElement.classList).toContain('high');
    expect(alertElements[1].nativeElement.classList).toContain('medium');
    expect(alertElements[2].nativeElement.classList).toContain('low');
  });

  it('should apply correct CSS classes based on status', () => {
    component.alerts = mockAlerts;
    fixture.detectChanges();

    const statusElements = fixture.debugElement.queryAll(By.css('.alert-status'));
    
    expect(statusElements[0].nativeElement.classList).toContain('pending');
    expect(statusElements[1].nativeElement.classList).toContain('sent');
    expect(statusElements[2].nativeElement.classList).toContain('sent');
  });

  it('should display correct alert information', () => {
    component.alerts = mockAlerts;
    fixture.detectChanges();

    const titleElements = fixture.debugElement.queryAll(By.css('.alert-title'));
    const locationElements = fixture.debugElement.queryAll(By.css('.alert-location'));
    const timeElements = fixture.debugElement.queryAll(By.css('.alert-time'));

    expect(titleElements[0].nativeElement.textContent).toBe('High PM2.5 Alert');
    expect(locationElements[0].nativeElement.textContent).toBe('Paris');
    expect(timeElements[0].nativeElement.textContent).toBe('2 hours ago');
  });

  it('should handle pagination when enabled', () => {
    component.alerts = mockAlerts;
    component.showPagination = true;
    component.itemsPerPage = 2;
    fixture.detectChanges();

    let alertElements = fixture.debugElement.queryAll(By.css('.alert-item'));
    expect(alertElements.length).toBe(2); // Should show only first 2 items

    // Test next page
    component.nextPage();
    fixture.detectChanges();

    alertElements = fixture.debugElement.queryAll(By.css('.alert-item'));
    expect(alertElements.length).toBe(1); // Should show only the last item
  });

  it('should handle pagination controls correctly', () => {
    component.alerts = mockAlerts;
    component.showPagination = true;
    component.itemsPerPage = 2;
    fixture.detectChanges();

    expect(component.hasPreviousPage).toBe(false);
    expect(component.hasNextPage).toBe(true);

    component.nextPage();
    fixture.detectChanges();

    expect(component.hasPreviousPage).toBe(true);
    expect(component.hasNextPage).toBe(false);
  });

  it('should hide favorites filter when disabled', () => {
    component.alerts = mockAlerts;
    component.showFavoritesFilter = false;
    fixture.detectChanges();

    const favoritesButton = fixture.debugElement.queryAll(By.css('.link-button'));
    expect(favoritesButton.length).toBe(1); // Only "View all alerts" button
  });

  it('should handle empty alerts array', () => {
    component.alerts = [];
    fixture.detectChanges();

    const alertElements = fixture.debugElement.queryAll(By.css('.alert-item'));
    expect(alertElements.length).toBe(0);
  });

  it('should handle undefined alerts', () => {
    component.alerts = undefined as any;
    fixture.detectChanges();

    const alertElements = fixture.debugElement.queryAll(By.css('.alert-item'));
    expect(alertElements.length).toBe(0);
  });
});