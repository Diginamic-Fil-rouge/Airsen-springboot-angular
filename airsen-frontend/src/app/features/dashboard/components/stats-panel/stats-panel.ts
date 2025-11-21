import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { UserStatsSnapshot } from '../../models/user-stats';
import { UserFavoriteResponse } from '@/shared/models/favorite.model';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

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
export class StatsPanelComponent implements OnChanges {
  @Input() stats!: UserStatsSnapshot;
  @Input() favorites: UserFavoriteResponse[] = [];
  @Input() showProgressBar = true;
  @Output() statClick = new EventEmitter<StatClickEvent>();

  // Chart Data
  public doughnutChartLabels: string[] = ['Bon', 'Moyen', 'Dégradé', 'Mauvais', 'Très Mauvais', 'Extrêmement Mauvais'];
  public doughnutChartData: ChartData<'doughnut'> = {
    labels: this.doughnutChartLabels,
    datasets: [
      { data: [0, 0, 0, 0, 0, 0], backgroundColor: ['#12b76a', '#f79009', '#f04438', '#d92d20', '#9b1c1c', '#7a271a'] }
    ]
  };
  public doughnutChartType: ChartType = 'doughnut';
  public doughnutChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false }
    }
  };

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

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['favorites'] && this.favorites) {
      this.updateChartData();
    }
  }

  private updateChartData(): void {
    const count = this.favorites.length;
    if (count === 0) {
       this.doughnutChartData.datasets[0].data = [0, 0, 0, 0, 0, 0];
       return;
    }

    // Mock distribution: All in "Bon" for now as we don't have AQI in favorites list
    this.doughnutChartData = {
      ...this.doughnutChartData,
      datasets: [{
          ...this.doughnutChartData.datasets[0],
          data: [count, 0, 0, 0, 0, 0]
      }]
    };
  }

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