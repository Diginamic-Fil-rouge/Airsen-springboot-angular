import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AdminService, Campaign } from '../../services/admin.service';

@Component({
  selector: 'app-campaign-list',
  templateUrl: './campaign-list.component.html',
  styleUrls: ['./campaign-list.component.scss'],
  standalone: false
})
export class CampaignListComponent implements OnInit, OnDestroy {
  campaigns: Campaign[] = [];
  loading = true;
  error: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadCampaigns();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCampaigns(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getCampaigns()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (campaigns) => {
          this.campaigns = campaigns;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Erreur lors du chargement des campagnes';
          this.loading = false;
          console.error('Error loading campaigns:', err);
        }
      });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'COMPLETED': return '#4CAF50';
      case 'SENDING': return '#2196F3';
      case 'DRAFT': return '#9E9E9E';
      case 'FAILED': return '#F44336';
      default: return '#9E9E9E';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'Terminée';
      case 'SENDING': return 'En cours';
      case 'DRAFT': return 'Brouillon';
      case 'FAILED': return 'Échec';
      default: return status;
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'check_circle';
      case 'SENDING': return 'send';
      case 'DRAFT': return 'edit';
      case 'FAILED': return 'error';
      default: return 'help';
    }
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

  getDeliveryRate(campaign: Campaign): number {
    if (campaign.totalRecipients === 0) return 0;
    return Math.round((campaign.sentCount / campaign.totalRecipients) * 100);
  }

  getDeliveryRateColor(rate: number): string {
    if (rate >= 95) return '#4CAF50';
    if (rate >= 80) return '#FF9800';
    return '#F44336';
  }
}
