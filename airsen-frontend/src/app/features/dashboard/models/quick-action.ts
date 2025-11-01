export type QuickActionKey = 'map' | 'alerts' | 'forum' | 'favorites' | 'export';

export interface QuickActionCard {
  title: string;
  subtitle: string;
  icon: string;
  action: QuickActionKey;
  badge?: string;
}