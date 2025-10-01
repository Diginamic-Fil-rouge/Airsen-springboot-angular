// User Model based on backend User entity
export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  postalAddress: string;
  postalCode: string;
  city: string;
  role: UserRole;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
  commune?: Commune;
  department?: Department;
  region?: Region;
}

export enum UserRole {
  VISITOR = 'VISITOR',
  USER = 'USER', 
  ADMIN = 'ADMIN'
}

export interface UserRegistrationRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  postalAddress: string;
  postalCode: string;
  city: string;
}

export interface UserUpdateRequest {
  firstName?: string;
  lastName?: string;
  postalAddress?: string;
  postalCode?: string;
  city?: string;
}

export interface UserProfile extends User {
  favoriteCommunes: Commune[];
  notificationPreferences: NotificationPreference[];
}

export interface NotificationPreference {
  id: number;
  userId: number;
  airQualityAlerts: boolean;
  weatherAlerts: boolean;
  emailNotifications: boolean;
  scope: NotificationScope;
}

export enum NotificationScope {
  COMMUNE = 'COMMUNE',
  DEPARTMENT = 'DEPARTMENT',
  REGION = 'REGION',
  FRANCE = 'FRANCE'
}

// Geographic entities for user location association
export interface Region {
  id: number;
  code: string;
  name: string;
}

export interface Department {
  id: number;
  code: string;
  name: string;
  regionId: number;
  region?: Region;
}

export interface Commune {
  id: number;
  inseeCode: string;
  name: string;
  postalCode: string;
  latitude: number;
  longitude: number;
  population: number;
  departmentId: number;
  department?: Department;
}