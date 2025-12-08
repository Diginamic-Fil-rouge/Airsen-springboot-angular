import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AdminService, AlertSignal } from '../../services/admin.service';

@Component({
  selector: 'app-alert-list',
  templateUrl: './alert-list.component.html',
  styleUrls: ['./alert-list.component.scss'],
  standalone: false
})
export class AlertListComponent implements OnInit, OnDestroy {
  alerts: AlertSignal[] = [];
  loading = true;
  error: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadAlerts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAlerts(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getAlertSignals()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (alerts) => {
          this.alerts = alerts;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Erreur lors du chargement des signaux d\'alerte';
          this.loading = false;
          console.error('Error loading alerts:', err);
        }
      });
  }

  getLevelColor(level: string): string {
    switch (level) {
      case 'ALERT': return '#F44336';
      case 'WATCH': return '#FF9800';
      case 'INFO': return '#2196F3';
      default: return '#9E9E9E';
    }
  }

  getLevelLabel(level: string): string {
    switch (level) {
      case 'ALERT': return 'Alerte';
      case 'WATCH': return 'Vigilance';
      case 'INFO': return 'Information';
      default: return level;
    }
  }

  getSourceLabel(source: string): string {
    switch (source) {
      case 'ATMO': return 'ATMO France';
      case 'WEATHER': return 'Météo';
      default: return source;
    }
  }

  getKindLabel(kind: string): string {
    const labels: { [key: string]: string } = {
      'AQI': 'Indice ATMO',
      'PM25': 'Particules fines PM2.5',
      'PM10': 'Particules PM10',
      'O3': 'Ozone',
      'HEAT': 'Canicule',
      'WIND': 'Vents forts',
      'RAIN': 'Fortes pluies'
    };
    return labels[kind] || kind;
  }

  getScopeLabel(scopeType: string, scopeId: string | undefined): string {
    if (!scopeId) return scopeType;

    switch (scopeType) {
      case 'FRANCE': return 'France entière';
      case 'REGION': return `Région ${scopeId}`;
      case 'DEPARTMENT': return `Département ${scopeId}`;
      case 'COMMUNE': return `Commune ${scopeId}`;
      default: return scopeType;
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }

  deleteAlert(alertId: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce signal d\'alerte ?')) {
      return;
    }

    this.adminService.deleteAlertSignal(alertId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.alerts = this.alerts.filter(a => a.id !== alertId);
        },
        error: (err) => {
          console.error('Error deleting alert:', err);
          alert('Erreur lors de la suppression du signal d\'alerte');
        }
      });
  }
}
