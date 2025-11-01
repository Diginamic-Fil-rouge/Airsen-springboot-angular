export interface AlertSummaryItem {
  id: number;
  title: string;
  location: string;
  severity: 'low' | 'medium' | 'high';
  status: 'pending' | 'sent' | 'failed';
  icon: string;
  timestamp: string;
}