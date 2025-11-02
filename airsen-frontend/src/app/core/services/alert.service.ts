import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { CampaignNotification } from '@/shared/models';

/**
 * AlertService - User Notification Retrieval
 *
 * Handles USER-facing notification functionality:
 * - Fetch CampaignNotifications sent to authenticated user
 * - View notifications from admin-broadcast campaigns
 *
 * Architecture (Admin-Centric Broadcast System):
 * 1. AlertSignal: Environmental event detected by system (ATMO/Weather APIs)
 * 2. NotificationCampaign: Admin creates broadcast campaign from signal or manually
 * 3. CampaignNotification: Individual email sent to user as part of campaign
 *
 * User Workflow:
 * - Users DO NOT create personal alerts
 * - Users receive CampaignNotifications when admin broadcasts to their geographic area
 * - This service fetches those received notifications
 *
 * Backend Endpoints:
 * - GET /api/v1/users/{userId}/notifications - Fetch user's received notifications
 * - GET /api/v1/users/{userId}/notifications/recent?limit=N - Recent notifications
 */
@Injectable({
  providedIn: "root",
})
export class AlertService {
  private readonly apiUrl = `${environment.apiUrl}/users`;
  private http = inject(HttpClient);

  //Fetches all notifications received by user.

  getUserNotifications(userId: number): Observable<CampaignNotification[]> {
    return this.http.get<CampaignNotification[]>(`${this.apiUrl}/${userId}/notifications`);
  }

  /**
   * Fetches recent notifications (limit N).
   * Use this for dashboard widget to show latest notifications.
   *
   * Backend Endpoint: GET /api/v1/users/{userId}/notifications/recent?limit=N
   */
  getRecentNotifications(userId: number, limit: number = 5): Observable<CampaignNotification[]> {
    const params = new HttpParams().set("limit", limit.toString());
    return this.http.get<CampaignNotification[]>(`${this.apiUrl}/${userId}/notifications/recent`, { params });
  }

  /**
   * Gets count of pending/unread notifications.
   * Use this for badge display in header/dashboard.
   */
  getPendingNotificationCount(userId: number): Observable<number> {
    return this.getUserNotifications(userId).pipe(
      map((notifications) => notifications.filter((n) => n.status === "PENDING").length)
    );
  }

  //Gets count of failed notifications (for user troubleshooting).
  getFailedNotificationCount(userId: number): Observable<number> {
    return this.getUserNotifications(userId).pipe(
      map((notifications) => notifications.filter((n) => n.status === "FAILED").length)
    );
  }
}
