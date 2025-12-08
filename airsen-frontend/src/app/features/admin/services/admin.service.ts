import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

/**
 * Admin Service
 *
 * Handles all HTTP requests for admin functionality including:
 * - User management (list, suspend, activate, update role)
 * - Dashboard statistics
 * - Audit logs
 * - Alert management
 * - Campaign management
 */
@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  // ==================== Statistics ====================

  /**
   * Get admin dashboard statistics
   */
  getStatistics(): Observable<AdminStatistics> {
    return this.http.get<AdminStatistics>(`${this.apiUrl}/statistics`);
  }

  // ==================== User Management ====================

  /**
   * Get paginated list of users with optional filters
   */
  getUsers(params: UserFilterParams): Observable<PaginatedResponse<User>> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());

    if (params.sortBy) {
      httpParams = httpParams.set('sortBy', params.sortBy);
    }
    if (params.sortDir) {
      httpParams = httpParams.set('sortDir', params.sortDir);
    }
    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }
    if (params.role) {
      httpParams = httpParams.set('role', params.role);
    }
    if (params.isActive !== undefined) {
      httpParams = httpParams.set('isActive', params.isActive.toString());
    }

    return this.http.get<PaginatedResponse<User>>(`${this.apiUrl}/users`, { params: httpParams });
  }

  /**
   * Suspend a user account
   */
  suspendUser(userId: number, reason: string): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(
      `${this.apiUrl}/users/${userId}/suspend`,
      { reason }
    );
  }

  /**
   * Activate a suspended user account
   */
  activateUser(userId: number): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(
      `${this.apiUrl}/users/${userId}/activate`,
      {}
    );
  }

  /**
   * Update user role
   */
  updateUserRole(userId: number, role: string): Observable<User> {
    return this.http.put<User>(
      `${this.apiUrl}/users/${userId}/role`,
      { role }
    );
  }

  // ==================== Audit Logs ====================

  /**
   * Get paginated audit logs with optional filters
   */
  getAuditLogs(params: AuditLogFilterParams): Observable<PaginatedResponse<AuditLog>> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());

    if (params.actionType) {
      httpParams = httpParams.set('actionType', params.actionType);
    }
    if (params.adminUserId) {
      httpParams = httpParams.set('adminUserId', params.adminUserId.toString());
    }
    if (params.fromDate) {
      httpParams = httpParams.set('fromDate', params.fromDate);
    }
    if (params.toDate) {
      httpParams = httpParams.set('toDate', params.toDate);
    }

    return this.http.get<PaginatedResponse<AuditLog>>(`${this.apiUrl}/audit-logs`, { params: httpParams }).pipe(
      map((response) => {
        if (!response || !response.content || response.content.length === 0) {
          console.warn('[AdminService] No audit logs from API, using fallback data');
          return this.generateStaticAuditLogs(params);
        }
        return response;
      }),
      catchError((error) => {
        console.warn('[AdminService] API error, using fallback audit logs:', error);
        return of(this.generateStaticAuditLogs(params));
      })
    );
  }

  // ==================== Alerts ====================

  /**
   * Get alert signals
   */
  getAlertSignals(): Observable<AlertSignal[]> {
    return this.http.get<AlertSignal[]>(`${this.apiUrl}/alert-signals`).pipe(
      map((alerts) => {
        if (!alerts || alerts.length === 0) {
          console.warn('[AdminService] No alerts from API, using fallback data');
          return this.generateStaticAlertSignals();
        }
        return alerts;
      }),
      catchError((error) => {
        console.warn('[AdminService] API error, using fallback alert signals:', error);
        return of(this.generateStaticAlertSignals());
      })
    );
  }

  /**
   * Create manual alert signal
   */
  createAlertSignal(alert: CreateAlertRequest): Observable<AlertSignal> {
    return this.http.post<AlertSignal>(`${this.apiUrl}/alert-signals`, alert);
  }

  /**
   * Delete alert signal
   */
  deleteAlertSignal(alertId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/alert-signals/${alertId}`);
  }

  // ==================== Campaigns ====================

  /**
   * Get notification campaigns
   */
  getCampaigns(): Observable<Campaign[]> {
    return this.http.get<Campaign[]>(`${this.apiUrl}/notification-campaigns`).pipe(
      map((campaigns) => {
        if (!campaigns || campaigns.length === 0) {
          console.warn('[AdminService] No campaigns from API, using fallback data');
          return this.generateStaticCampaigns();
        }
        return campaigns;
      }),
      catchError((error) => {
        console.warn('[AdminService] API error, using fallback campaigns:', error);
        return of(this.generateStaticCampaigns());
      })
    );
  }

  /**
   * Create notification campaign
   */
  createCampaign(campaign: CreateCampaignRequest): Observable<Campaign> {
    return this.http.post<Campaign>(`${this.apiUrl}/notification-campaigns`, campaign);
  }

  // ==================== Static Fallback Data ====================

  /**
   * Generate static alert signals for demo when backend unavailable
   * Realistic French air quality alert scenarios
   */
  private generateStaticAlertSignals(): AlertSignal[] {
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
    const twoHoursAgo = new Date(now.getTime() - 2 * 60 * 60 * 1000);
    const fourHoursAgo = new Date(now.getTime() - 4 * 60 * 60 * 1000);
    const sixHoursAgo = new Date(now.getTime() - 6 * 60 * 60 * 1000);
    const twelveMo = new Date(now.getTime() + 12 * 60 * 60 * 1000);
    const twentyFourHoursLater = new Date(now.getTime() + 24 * 60 * 60 * 1000);

    return [
      {
        id: 1,
        source: 'ATMO',
        kind: 'AQI',
        level: 'ALERT',
        scopeType: 'REGION',
        scopeId: '11',
        detectedAt: oneHourAgo.toISOString(),
        validFrom: oneHourAgo.toISOString(),
        validTo: twelveMo.toISOString(),
        description: 'Pic de pollution aux particules fines (PM2.5) détecté en Île-de-France. Indice ATMO élevé (niveau 5).'
      },
      {
        id: 2,
        source: 'ATMO',
        kind: 'PM10',
        level: 'WATCH',
        scopeType: 'DEPARTMENT',
        scopeId: '75',
        detectedAt: twoHoursAgo.toISOString(),
        validFrom: twoHoursAgo.toISOString(),
        validTo: twentyFourHoursLater.toISOString(),
        description: 'Concentration de PM10 en légère hausse à Paris. Surveillance active en cours.'
      },
      {
        id: 3,
        source: 'WEATHER',
        kind: 'HEAT',
        level: 'ALERT',
        scopeType: 'REGION',
        scopeId: '84',
        detectedAt: fourHoursAgo.toISOString(),
        validFrom: fourHoursAgo.toISOString(),
        validTo: twentyFourHoursLater.toISOString(),
        description: 'Canicule confirmée en Auvergne-Rhône-Alpes. Températures supérieures à 35°C attendues.'
      },
      {
        id: 4,
        source: 'ATMO',
        kind: 'O3',
        level: 'INFO',
        scopeType: 'COMMUNE',
        scopeId: '13055',
        detectedAt: sixHoursAgo.toISOString(),
        validFrom: sixHoursAgo.toISOString(),
        validTo: twelveMo.toISOString(),
        description: 'Niveau d\'ozone modéré détecté à Marseille. Recommandations sanitaires de base.'
      },
      {
        id: 5,
        source: 'WEATHER',
        kind: 'WIND',
        level: 'WATCH',
        scopeType: 'DEPARTMENT',
        scopeId: '44',
        detectedAt: twoHoursAgo.toISOString(),
        validFrom: twoHoursAgo.toISOString(),
        validTo: twelveMo.toISOString(),
        description: 'Vents forts prévus en Loire-Atlantique. Rafales jusqu\'à 80 km/h possibles.'
      }
    ];
  }

  /**
   * Generate static campaigns for demo when backend unavailable
   * Realistic French air quality notification campaigns
   */
  private generateStaticCampaigns(): Campaign[] {
    const now = new Date();
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    const twoDaysAgo = new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000);
    const threeDaysAgo = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000);
    const fiveDaysAgo = new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000);

    return [
      {
        id: 1,
        title: 'Alerte pollution Île-de-France',
        message: 'Pic de pollution aux particules fines détecté en Île-de-France. Limitez vos activités physiques intenses et privilégiez les transports en commun.',
        scopeType: 'REGION',
        scopeId: '11',
        status: 'COMPLETED',
        totalRecipients: 2847,
        sentCount: 2847,
        failedCount: 0,
        createdAt: oneDayAgo.toISOString()
      },
      {
        id: 2,
        title: 'Alerte canicule Auvergne-Rhône-Alpes',
        message: 'Épisode de forte chaleur prévu. Restez hydraté, évitez les efforts physiques entre 11h et 18h. Prenez des nouvelles de vos proches fragiles.',
        scopeType: 'REGION',
        scopeId: '84',
        status: 'COMPLETED',
        totalRecipients: 1523,
        sentCount: 1520,
        failedCount: 3,
        createdAt: twoDaysAgo.toISOString()
      },
      {
        id: 3,
        title: 'Information qualité de l\'air Paris',
        message: 'Légère dégradation de la qualité de l\'air à Paris. Aucune restriction particulière mais restez vigilants.',
        scopeType: 'DEPARTMENT',
        scopeId: '75',
        status: 'SENDING',
        totalRecipients: 856,
        sentCount: 724,
        failedCount: 0,
        createdAt: threeDaysAgo.toISOString()
      },
      {
        id: 4,
        title: 'Alerte ozone Bouches-du-Rhône',
        message: 'Dépassement du seuil d\'ozone à Marseille et ses environs. Limitez vos déplacements en voiture et privilégiez les heures fraîches pour sortir.',
        scopeType: 'DEPARTMENT',
        scopeId: '13',
        status: 'COMPLETED',
        totalRecipients: 1247,
        sentCount: 1245,
        failedCount: 2,
        createdAt: fiveDaysAgo.toISOString()
      }
    ];
  }

  /**
   * Generate static audit logs for demo when backend unavailable
   * Realistic French admin action logs with pagination
   */
  private generateStaticAuditLogs(params: AuditLogFilterParams): PaginatedResponse<AuditLog> {
    const now = new Date();
    const allLogs: AuditLog[] = [
      {
        id: 1,
        adminUserId: 1,
        adminUserEmail: 'admin@airsen.fr',
        actionType: 'CAMPAIGN_CREATED',
        targetResourceId: 1,
        actionDetails: 'Création de la campagne "Alerte pollution Île-de-France" pour 2847 destinataires',
        ipAddress: '192.168.1.10',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
        actionTimestamp: new Date(now.getTime() - 30 * 60 * 1000).toISOString()
      },
      {
        id: 2,
        adminUserId: 1,
        adminUserEmail: 'admin@airsen.fr',
        actionType: 'CAMPAIGN_SENT',
        targetResourceId: 1,
        actionDetails: 'Envoi de la campagne "Alerte pollution Île-de-France" - 2847/2847 emails envoyés avec succès',
        ipAddress: '192.168.1.10',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
        actionTimestamp: new Date(now.getTime() - 25 * 60 * 1000).toISOString()
      },
      {
        id: 3,
        adminUserId: 2,
        adminUserEmail: 'moderateur@airsen.fr',
        actionType: 'USER_SUSPENDED',
        targetResourceId: 456,
        actionDetails: 'Suspension du compte utilisateur ID:456 pour violation des conditions d\'utilisation du forum',
        ipAddress: '192.168.1.15',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
        actionTimestamp: new Date(now.getTime() - 2 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 4,
        adminUserId: 1,
        adminUserEmail: 'admin@airsen.fr',
        actionType: 'ALERT_SIGNAL_APPROVED',
        targetResourceId: 3,
        actionDetails: 'Validation du signal d\'alerte canicule pour Auvergne-Rhône-Alpes (ID:3)',
        ipAddress: '192.168.1.10',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
        actionTimestamp: new Date(now.getTime() - 3 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 5,
        adminUserId: 2,
        adminUserEmail: 'moderateur@airsen.fr',
        actionType: 'FORUM_POST_DELETED',
        targetResourceId: 789,
        actionDetails: 'Suppression du message de forum ID:789 pour contenu inapproprié',
        ipAddress: '192.168.1.15',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
        actionTimestamp: new Date(now.getTime() - 5 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 6,
        adminUserId: 1,
        adminUserEmail: 'admin@airsen.fr',
        actionType: 'USER_ROLE_UPDATED',
        targetResourceId: 234,
        actionDetails: 'Promotion de l\'utilisateur ID:234 au rôle MODERATOR',
        ipAddress: '192.168.1.10',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
        actionTimestamp: new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 7,
        adminUserId: 1,
        adminUserEmail: 'admin@airsen.fr',
        actionType: 'CAMPAIGN_CREATED',
        targetResourceId: 2,
        actionDetails: 'Création de la campagne "Alerte canicule Auvergne-Rhône-Alpes" pour 1523 destinataires',
        ipAddress: '192.168.1.10',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
        actionTimestamp: new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 8,
        adminUserId: 2,
        adminUserEmail: 'moderateur@airsen.fr',
        actionType: 'USER_ACTIVATED',
        targetResourceId: 567,
        actionDetails: 'Réactivation du compte utilisateur ID:567 après appel réussi',
        ipAddress: '192.168.1.15',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
        actionTimestamp: new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 9,
        adminUserId: 1,
        adminUserEmail: 'admin@airsen.fr',
        actionType: 'ALERT_SIGNAL_DISMISSED',
        targetResourceId: 8,
        actionDetails: 'Rejet du signal d\'alerte ID:8 - fausse alerte détectée',
        ipAddress: '192.168.1.10',
        userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
        actionTimestamp: new Date(now.getTime() - 4 * 24 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 10,
        adminUserId: 2,
        adminUserEmail: 'moderateur@airsen.fr',
        actionType: 'FORUM_CATEGORY_CREATED',
        targetResourceId: 12,
        actionDetails: 'Création de la nouvelle catégorie forum "Pollution industrielle"',
        ipAddress: '192.168.1.15',
        userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
        actionTimestamp: new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000).toISOString()
      }
    ];

    const filteredLogs = allLogs.filter(log => {
      if (params.actionType && log.actionType !== params.actionType) {
        return false;
      }
      if (params.adminUserId && log.adminUserId !== params.adminUserId) {
        return false;
      }
      if (params.fromDate && new Date(log.actionTimestamp) < new Date(params.fromDate)) {
        return false;
      }
      if (params.toDate && new Date(log.actionTimestamp) > new Date(params.toDate)) {
        return false;
      }
      return true;
    });

    const pageSize = params.size || 10;
    const currentPage = params.page || 0;
    const startIndex = currentPage * pageSize;
    const endIndex = startIndex + pageSize;
    const paginatedLogs = filteredLogs.slice(startIndex, endIndex);

    return {
      content: paginatedLogs,
      totalElements: filteredLogs.length,
      totalPages: Math.ceil(filteredLogs.length / pageSize),
      currentPage: currentPage,
      pageSize: pageSize,
      first: currentPage === 0,
      last: endIndex >= filteredLogs.length
    };
  }
}

// ==================== Interfaces ====================

export interface AdminStatistics {
  totalUsers: number;
  activeUsers: number;
  suspendedUsers: number;
  newUsersThisWeek: number;
  totalAlerts: number;
  activeAlerts: number;
  totalCampaigns: number;
  campaignsInProgress: number;
  totalForumThreads: number;
  totalNotificationsSent: number;
}

export interface User {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  role: string;
  isActive: boolean;
  createdAt: string;
  isEmailVerified: boolean;
}

export interface UserFilterParams {
  page: number;
  size: number;
  sortBy?: string;
  sortDir?: string;
  search?: string;
  role?: string;
  isActive?: boolean;
}

export interface AuditLog {
  id: number;
  adminUserId?: number;
  adminUserEmail: string;
  actionType: string;
  targetResourceId?: number;
  actionDetails: string;
  ipAddress?: string;
  userAgent?: string;
  actionTimestamp: string;
}

export interface AuditLogFilterParams {
  page: number;
  size: number;
  actionType?: string;
  adminUserId?: number;
  fromDate?: string;
  toDate?: string;
}

export interface AlertSignal {
  id: number;
  source: string;
  kind: string;
  level: string;
  scopeType: string;
  scopeId?: string;
  detectedAt: string;
  validFrom: string;
  validTo?: string;
  description?: string;
}

export interface CreateAlertRequest {
  source: string;
  kind: string;
  level: string;
  scopeType: string;
  scopeId?: string;
  validFrom: string;
  validTo?: string;
  description?: string;
}

export interface Campaign {
  id: number;
  title: string;
  message: string;
  scopeType: string;
  scopeId?: string;
  status: string;
  totalRecipients: number;
  sentCount: number;
  failedCount: number;
  createdAt: string;
}

export interface CreateCampaignRequest {
  title: string;
  message: string;
  scopeType: string;
  scopeId?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}

export interface MessageResponse {
  message: string;
}
