import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
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

    return this.http.get<PaginatedResponse<AuditLog>>(`${this.apiUrl}/audit-logs`, { params: httpParams });
  }

  // ==================== Alerts ====================

  /**
   * Get alert signals
   */
  getAlertSignals(): Observable<AlertSignal[]> {
    return this.http.get<AlertSignal[]>(`${this.apiUrl}/alert-signals`);
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
    return this.http.get<Campaign[]>(`${this.apiUrl}/notification-campaigns`);
  }

  /**
   * Create notification campaign
   */
  createCampaign(campaign: CreateCampaignRequest): Observable<Campaign> {
    return this.http.post<Campaign>(`${this.apiUrl}/notification-campaigns`, campaign);
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
