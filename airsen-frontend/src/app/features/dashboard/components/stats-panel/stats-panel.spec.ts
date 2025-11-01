import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatsPanelComponent, StatClickEvent } from './stats-panel';
import { UserStatsSnapshot } from '../../models/user-stats';
import { By } from '@angular/platform-browser';

describe('StatsPanelComponent', () => {
  let component: StatsPanelComponent;
  let fixture: ComponentFixture<StatsPanelComponent>;

  const mockStats: UserStatsSnapshot = {
    favoriteIndicators: 5,
    alertsReceived: 12,
    lastExport: '2024-01-15',
    forumPosts: 3,
    profileCompletion: 75
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatsPanelComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(StatsPanelComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display stats when provided', () => {
    component.stats = mockStats;
    fixture.detectChanges();

    const statElements = fixture.debugElement.queryAll(By.css('.stat-item'));
    expect(statElements.length).toBe(5); // All 5 stats should be displayed
  });

  it('should display correct stat labels and values', () => {
    component.stats = mockStats;
    fixture.detectChanges();

    const labelElements = fixture.debugElement.queryAll(By.css('.stat-label'));
    const valueElements = fixture.debugElement.queryAll(By.css('.stat-value'));

    expect(labelElements[0].nativeElement.textContent).toBe('Favorite indicators monitored');
    expect(valueElements[0].nativeElement.textContent).toBe('5');
    
    expect(labelElements[1].nativeElement.textContent).toBe('Personalized alerts received');
    expect(valueElements[1].nativeElement.textContent).toBe('12');
  });

  it('should emit statClick event for clickable stats', () => {
    component.stats = mockStats;
    fixture.detectChanges();

    const spy = spyOn(component.statClick, 'emit');
    const firstStat = fixture.debugElement.query(By.css('.stat-item.clickable'));
    
    firstStat.triggerEventHandler('click', null);
    
    expect(spy).toHaveBeenCalledWith({
      statKey: 'favoriteIndicators',
      value: 5
    });
  });

  it('should apply clickable class only to clickable stats', () => {
    component.stats = mockStats;
    fixture.detectChanges();

    const clickableElements = fixture.debugElement.queryAll(By.css('.stat-item.clickable'));
    expect(clickableElements.length).toBe(3); // favoriteIndicators, alertsReceived, forumPosts
  });

  it('should display progress bar when enabled', () => {
    component.stats = mockStats;
    component.showProgressBar = true;
    fixture.detectChanges();

    const progressCard = fixture.debugElement.query(By.css('.progress-card'));
    expect(progressCard).toBeTruthy();

    const progressFill = fixture.debugElement.query(By.css('.progress-fill'));
    expect(progressFill.nativeElement.style.width).toBe('75%');
  });

  it('should hide progress bar when disabled', () => {
    component.stats = mockStats;
    component.showProgressBar = false;
    fixture.detectChanges();

    const progressCard = fixture.debugElement.query(By.css('.progress-card'));
    expect(progressCard).toBeFalsy();
  });

  it('should handle progress percentage correctly', () => {
    component.stats = { ...mockStats, profileCompletion: 150 }; // Test over 100%
    fixture.detectChanges();

    const progressFill = fixture.debugElement.query(By.css('.progress-fill'));
    expect(component.getProgressPercentage(150)).toBe(100);
  });

  it('should handle negative progress percentage', () => {
    component.stats = { ...mockStats, profileCompletion: -10 }; // Test negative value
    fixture.detectChanges();

    expect(component.getProgressPercentage(-10)).toBe(0);
  });

  it('should handle undefined stats', () => {
    component.stats = undefined as any;
    fixture.detectChanges();

    const statElements = fixture.debugElement.queryAll(By.css('.stat-item'));
    expect(statElements.length).toBe(0);
  });

  it('should handle null stats', () => {
    component.stats = null as any;
    fixture.detectChanges();

    const statElements = fixture.debugElement.queryAll(By.css('.stat-item'));
    expect(statElements.length).toBe(0);
  });

  it('should not emit statClick for non-clickable stats', () => {
    component.stats = mockStats;
    fixture.detectChanges();

    const spy = spyOn(component.statClick, 'emit');
    const nonClickableStat = fixture.debugElement.queryAll(By.css('.stat-item:not(.clickable)'))[0];
    
    nonClickableStat.triggerEventHandler('click', null);
    
    expect(spy).not.toHaveBeenCalled();
  });

  it('should identify clickable stats correctly', () => {
    expect(component.isStatClickable('favoriteIndicators')).toBe(true);
    expect(component.isStatClickable('alertsReceived')).toBe(true);
    expect(component.isStatClickable('forumPosts')).toBe(true);
    expect(component.isStatClickable('lastExport')).toBe(false);
    expect(component.isStatClickable('profileCompletion')).toBe(false);
  });
});