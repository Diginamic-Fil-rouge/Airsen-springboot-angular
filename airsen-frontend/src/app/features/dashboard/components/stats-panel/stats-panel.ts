import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { UserStatsSnapshot } from '../../models/user-stats';

export interface StatClickEvent {
  statKey: keyof UserStatsSnapshot;
  value: number | string;
}

@Component({
  selector: 'app-stats-panel',
  standalone: false,
  templateUrl: './stats-panel.html',
  styleUrls: ['./stats-panel.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatsPanelComponent {
  @Input() stats!: UserStatsSnapshot;
  @Input() showProgressBar = true;
  @Output() statClick = new EventEmitter<StatClickEvent>();

  readonly statLabels: Record<keyof UserStatsSnapshot, string> = {
    favoriteIndicators: 'Indicateurs favoris surveillés',
    alertsReceived: 'Alertes personnalisées reçues',
    lastExport: 'Dernier export',
    forumPosts: 'Posts sur le forum',
    profileCompletion: 'Profil complété'
  };

  readonly statKeys: (keyof UserStatsSnapshot)[] = [
    'favoriteIndicators',
    'alertsReceived', 
    'lastExport',
    'forumPosts',
    'profileCompletion'
  ];

  readonly clickableStats: (keyof UserStatsSnapshot)[] = [
    'favoriteIndicators',
    'alertsReceived',
    'forumPosts'
  ];

  onStatClick(statKey: keyof UserStatsSnapshot): void {
    if (this.clickableStats.includes(statKey)) {
      this.statClick.emit({
        statKey,
        value: this.stats[statKey]
      });
    }
  }

  isStatClickable(statKey: keyof UserStatsSnapshot): boolean {
    return this.clickableStats.includes(statKey);
  }

  getProgressPercentage(value: number): number {
    return Math.min(Math.max(value, 0), 100);
  }
}