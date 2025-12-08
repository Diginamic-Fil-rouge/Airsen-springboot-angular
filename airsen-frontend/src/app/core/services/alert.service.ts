import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { CampaignNotification, NotificationDeliveryStatus } from '@/shared/models';

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
  // Falls back to static data if API fails
  getUserNotifications(userId: number): Observable<CampaignNotification[]> {
    return this.http.get<CampaignNotification[]>(`${this.apiUrl}/${userId}/notifications`).pipe(
      catchError((error) => {
        console.warn('[AlertService] API error, using fallback notifications:', error);
        return of(this.generateStaticNotifications());
      })
    );
  }

  /**
   * Fetches recent notifications (limit N).
   * Use this for dashboard widget to show latest notifications.
   * Falls back to static data if API fails
   *
   * Backend Endpoint: GET /api/v1/users/{userId}/notifications/recent?limit=N
   */
  getRecentNotifications(userId: number, limit = 5): Observable<CampaignNotification[]> {
    const params = new HttpParams().set("limit", limit.toString());
    return this.http.get<CampaignNotification[]>(`${this.apiUrl}/${userId}/notifications/recent`, { params }).pipe(
      catchError((error) => {
        console.warn('[AlertService] API error, using fallback recent notifications:', error);
        return of(this.generateStaticNotifications().slice(0, limit));
      })
    );
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

  /**
   * Generate static fallback notifications for demo
   * Paris user example: 3 recent notifications about air quality
   */
  private generateStaticNotifications(): CampaignNotification[] {
    const now = new Date();
    const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    const twoDaysAgo = new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000);
    const threeDaysAgo = new Date(now.getTime() - 3 * 24 * 60 * 60 * 1000);

    return [
      {
        id: 1,
        campaignId: 101,
        userId: 1,
        recipientEmail: 'demo@airsen.fr',
        status: 'SENT' as NotificationDeliveryStatus,
        sentAt: oneDayAgo,
        failedAt: null,
        errorMessage: null,
        createdAt: oneDayAgo
      },
      {
        id: 2,
        campaignId: 102,
        userId: 1,
        recipientEmail: 'demo@airsen.fr',
        status: 'SENT' as NotificationDeliveryStatus,
        sentAt: twoDaysAgo,
        failedAt: null,
        errorMessage: null,
        createdAt: twoDaysAgo
      },
      {
        id: 3,
        campaignId: 103,
        userId: 1,
        recipientEmail: 'demo@airsen.fr',
        status: 'SENT' as NotificationDeliveryStatus,
        sentAt: threeDaysAgo,
        failedAt: null,
        errorMessage: null,
        createdAt: threeDaysAgo
      }
    ];
  }
}
