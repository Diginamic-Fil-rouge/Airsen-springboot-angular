export interface Alert {
  id: number;
  title: string;
  message: string;
  severity: AlertSeverity;
  alertType: AlertType;
  geographicScope: GeographicScope;
  validFrom: Date;
  validUntil?: Date;
  createdAt: Date;
}

export enum AlertSeverity {
  INFO = "INFO",
  WARNING = "WARNING",
  CRITICAL = "CRITICAL",
}
export enum AlertType {
  AIR_QUALITY = "AIR_QUALITY",
  WEATHER = "WEATHER",
  SYSTEM = "SYSTEM",
}

export enum GeographicScope {
  FRANCE = "FRANCE",
  REGION = "REGION",
  DEPARTMENT = "DEPARTMENT",
  COMMUNE = "COMMUNE",
}

export interface AlertFilterRequest {
  page?: number;
  size?: number;
  severity?: AlertSeverity;
  alertType?: AlertType;
  geographicScope?: GeographicScope;
}

export interface AlertPage {
  content: Alert[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}
