/**
 * Architecture:
 * 1. AlertSignal: Environmental event detected by system (ATMO/Weather APIs)
 * 2. NotificationCampaign: Admin-created broadcast campaign
 * 3. Notification: Individual email sent to user as part of campaign
 *
 * Workflow:
 * - System detects AlertSignal → Admin reviews → Creates NotificationCampaign
 * - Campaign generates Notifications → Admin triggers send → Users receive emails
 */

// ==================== ENUMS ====================

export enum AlertSignalSource {
  ATMO = "ATMO",
  WEATHER = "WEATHER"
}

export enum AlertSignalKind {
  AQI = "AQI",
  PM25 = "PM25",
  PM10 = "PM10",
  HEAT = "HEAT",
  WIND = "WIND",
  RAIN = "RAIN"
}

export enum AlertSignalLevel {
  INFO = "INFO",
  WATCH = "WATCH",
  ALERT = "ALERT"
}

export enum GeographicScopeType {
  FRANCE = "FRANCE",
  REGION = "REGION",
  DEPARTMENT = "DEPARTMENT",
  COMMUNE = "COMMUNE"
}

export enum NotificationCampaignStatus {
  DRAFT = "DRAFT",
  SENDING = "SENDING",
  COMPLETED = "COMPLETED",
  FAILED = "FAILED"
}

export enum NotificationDeliveryStatus {
  PENDING = "PENDING",
  SENT = "SENT",
  FAILED = "FAILED"
}

// ==================== RESPONSE MODELS ====================

/**
 * AlertSignal - Detected environmental signal (not user-facing)
 * Admin reviews signals in Alert Center before creating campaigns
 */
export interface AlertSignal {
  id: number;
  source: AlertSignalSource;
  kind: AlertSignalKind;
  level: AlertSignalLevel;
  scopeType: GeographicScopeType;
  scopeId: number | null;
  summary: string;
  details: string;
  detectedAt: Date;
  validFrom: Date;
  validTo: Date | null;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * NotificationCampaign - Admin-created broadcast campaign
 */
export interface NotificationCampaign {
  id: number;
  title: string;
  message: string;
  scopeType: GeographicScopeType;
  scopeId: number | null;
  createdBy: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
  };
  createdAt: Date;
  status: NotificationCampaignStatus;
  totalRecipients: number;
  sentCount: number;
  failedCount: number;
  deliveryRate: number;
}

/**
 * CampaignNotification - Individual notification sent to user
 */
export interface CampaignNotification {
  id: number;
  campaignId: number;
  userId: number;
  recipientEmail: string;
  status: NotificationDeliveryStatus;
  sentAt: Date | null;
  failedAt: Date | null;
  errorMessage: string | null;
  createdAt: Date;
}

// ==================== REQUEST MODELS ====================

/**
 * CreateManualSignalRequest - Admin creates manual environmental signal
 */
export interface CreateManualSignalRequest {
  source: AlertSignalSource;
  kind: AlertSignalKind;
  level: AlertSignalLevel;
  scopeType: GeographicScopeType;
  scopeId: number | null;
  summary: string;
  details: string;
  validFrom: Date;
  validTo: Date | null;
}

/**
 * CreateCampaignRequest - Admin creates notification campaign
 */
export interface CreateCampaignRequest {
  title: string;
  message: string;
  scopeType: GeographicScopeType;
  scopeId: number | null;
}

// ==================== STATISTICS MODELS ====================

/**
 * AlertSignalStatistics - Admin dashboard statistics for signals
 */
export interface AlertSignalStatistics {
  totalSignals: number;
  bySource: Record<string, number>;
  byLevel: Record<string, number>;
  lastDetectionAt: Date | null;
}

/**
 * CampaignStatistics - Delivery metrics for campaign
 */
export interface CampaignStatistics {
  campaignId: number;
  totalRecipients: number;
  sentCount: number;
  failedCount: number;
  pendingCount: number;
  deliveryRate: number;
}

// ==================== PAGINATED RESPONSES ====================

export interface AlertSignalPage {
  content: AlertSignal[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface NotificationCampaignPage {
  content: NotificationCampaign[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// ==================== FILTER PARAMS ====================

export interface AlertSignalFilterParams {
  source?: AlertSignalSource;
  kind?: AlertSignalKind;
  level?: AlertSignalLevel;
  scopeType?: GeographicScopeType;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CampaignFilterParams {
  status?: NotificationCampaignStatus;
  scopeType?: GeographicScopeType;
  createdById?: number;
  page?: number;
  size?: number;
  sort?: string;
}
