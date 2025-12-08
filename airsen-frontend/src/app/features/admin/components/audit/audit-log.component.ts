import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AdminService, AuditLog, PaginatedResponse } from '../../services/admin.service';

@Component({
  selector: 'app-audit-log',
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.scss'],
  standalone: false
})
export class AuditLogComponent implements OnInit, OnDestroy {
  auditLogs: AuditLog[] = [];
  loading = true;
  error: string | null = null;

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  private destroy$ = new Subject<void>();

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadAuditLogs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAuditLogs(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getAuditLogs({
      page: this.currentPage,
      size: this.pageSize
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: PaginatedResponse<AuditLog>) => {
          this.auditLogs = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Erreur lors du chargement des logs d\'audit';
          this.loading = false;
          console.error('Error loading audit logs:', err);
        }
      });
  }

  getActionTypeColor(actionType: string): string {
    if (actionType.includes('CREATED')) return '#4CAF50';
    if (actionType.includes('DELETED') || actionType.includes('SUSPENDED')) return '#F44336';
    if (actionType.includes('UPDATED') || actionType.includes('ACTIVATED')) return '#2196F3';
    if (actionType.includes('SENT')) return '#9C27B0';
    return '#9E9E9E';
  }

  getActionTypeIcon(actionType: string): string {
    if (actionType.includes('CREATED')) return 'add_circle';
    if (actionType.includes('DELETED')) return 'delete';
    if (actionType.includes('UPDATED')) return 'edit';
    if (actionType.includes('SENT')) return 'send';
    if (actionType.includes('SUSPENDED')) return 'block';
    if (actionType.includes('ACTIVATED')) return 'check_circle';
    if (actionType.includes('APPROVED')) return 'thumb_up';
    if (actionType.includes('DISMISSED')) return 'thumb_down';
    return 'info';
  }

  getActionTypeLabel(actionType: string): string {
    const labels: { [key: string]: string } = {
      'CAMPAIGN_CREATED': 'Campagne créée',
      'CAMPAIGN_SENT': 'Campagne envoyée',
      'USER_SUSPENDED': 'Utilisateur suspendu',
      'USER_ACTIVATED': 'Utilisateur activé',
      'USER_ROLE_UPDATED': 'Rôle modifié',
      'ALERT_SIGNAL_APPROVED': 'Signal approuvé',
      'ALERT_SIGNAL_DISMISSED': 'Signal rejeté',
      'FORUM_POST_DELETED': 'Post supprimé',
      'FORUM_CATEGORY_CREATED': 'Catégorie créée'
    };
    return labels[actionType] || actionType;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).format(date);
  }

  formatRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInMinutes < 1) return 'À l\'instant';
    if (diffInMinutes < 60) return `Il y a ${diffInMinutes} min`;
    if (diffInHours < 24) return `Il y a ${diffInHours}h`;
    if (diffInDays === 1) return 'Hier';
    if (diffInDays < 7) return `Il y a ${diffInDays} jours`;
    return this.formatDate(dateString);
  }

  onPageChange(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages) {
      this.currentPage = newPage;
      this.loadAuditLogs();
    }
  }

  getPaginationRange(): number[] {
    const range: number[] = [];
    const maxPages = 5;
    let startPage = Math.max(0, this.currentPage - Math.floor(maxPages / 2));
    const endPage = Math.min(this.totalPages, startPage + maxPages);

    if (endPage - startPage < maxPages) {
      startPage = Math.max(0, endPage - maxPages);
    }

    for (let i = startPage; i < endPage; i++) {
      range.push(i);
    }
    return range;
  }
}
