import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuickActionsComponent } from './quick-actions';
import { QuickActionCard, QuickActionKey } from '../../models/quick-action';
import { By } from '@angular/platform-browser';
import { MatIconModule } from '@angular/material/icon';

describe('QuickActionsComponent', () => {
  let component: QuickActionsComponent;
  let fixture: ComponentFixture<QuickActionsComponent>;

  const mockActions: QuickActionCard[] = [
    {
      title: 'View Map',
      subtitle: 'Jump to live air quality layers.',
      icon: 'map',
      action: 'map'
    },
    {
      title: 'My Notifications',
      subtitle: '2 new alerts waiting',
      icon: 'notifications_active',
      action: 'alerts',
      badge: '2'
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuickActionsComponent, MatIconModule]
    }).compileComponents();

    fixture = TestBed.createComponent(QuickActionsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display quick actions when actions are provided', () => {
    component.actions = mockActions;
    fixture.detectChanges();

    const actionElements = fixture.debugElement.queryAll(By.css('.quick-card'));
    expect(actionElements.length).toBe(2);
  });

  it('should display action badges when provided', () => {
    component.actions = mockActions;
    fixture.detectChanges();

    const badgeElements = fixture.debugElement.queryAll(By.css('.quick-badge'));
    expect(badgeElements.length).toBe(1);
    expect(badgeElements[0].nativeElement.textContent).toBe('2');
  });

  it('should emit actionClick event when action is clicked', () => {
    component.actions = mockActions;
    fixture.detectChanges();

    const spy = spyOn(component.actionClick, 'emit');
    const firstAction = fixture.debugElement.query(By.css('.quick-card'));
    
    firstAction.triggerEventHandler('click', null);
    
    expect(spy).toHaveBeenCalledWith('map');
  });

  it('should not display badges when not provided', () => {
    const actionsWithoutBadges: QuickActionCard[] = [
      {
        title: 'View Map',
        subtitle: 'Jump to live air quality layers.',
        icon: 'map',
        action: 'map'
      }
    ];

    component.actions = actionsWithoutBadges;
    fixture.detectChanges();

    const badgeElements = fixture.debugElement.queryAll(By.css('.quick-badge'));
    expect(badgeElements.length).toBe(0);
  });

  it('should handle empty actions array', () => {
    component.actions = [];
    fixture.detectChanges();

    const actionElements = fixture.debugElement.queryAll(By.css('.quick-card'));
    expect(actionElements.length).toBe(0);
  });

  it('should handle undefined actions', () => {
    component.actions = undefined as any;
    fixture.detectChanges();

    const actionElements = fixture.debugElement.queryAll(By.css('.quick-card'));
    expect(actionElements.length).toBe(0);
  });

  it('should display correct action titles and subtitles', () => {
    component.actions = mockActions;
    fixture.detectChanges();

    const titleElements = fixture.debugElement.queryAll(By.css('.quick-title'));
    const subtitleElements = fixture.debugElement.queryAll(By.css('.quick-subtitle'));

    expect(titleElements[0].nativeElement.textContent).toBe('View Map');
    expect(subtitleElements[0].nativeElement.textContent).toBe('Jump to live air quality layers.');
    
    expect(titleElements[1].nativeElement.textContent).toBe('My Notifications');
    expect(subtitleElements[1].nativeElement.textContent).toBe('2 new alerts waiting');
  });
});