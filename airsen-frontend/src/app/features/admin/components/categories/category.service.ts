import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

/**
 * Forum thread data model
 */
export interface ForumThread {
  id: string;
  title: string;
  content?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Forum category data model
 */
export interface ForumCategory {
  id: string | number;
  name: string;
  description: string;
  color?: string;
  threads?: ForumThread[];
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Request payload for creating a category
 */
export interface CreateCategoryRequest {
  name: string;
  description: string;
  color: string;
}

/**
 * Request payload for updating a category
 */
export interface UpdateCategoryRequest {
  name: string;
  description: string;
  color?: string;
}

/**
 * Category Service
 * Handles CRUD operations for forum categories
 */
@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private readonly API_URL = `${environment.apiUrl}/forum/categories`;

  constructor(private http: HttpClient) { }

  /**
   * Get all forum categories
   */
  getAll(): Observable<ForumCategory[]> {
    return this.http.get<ForumCategory[]>(this.API_URL);
  }

  /**
   * Get a specific category by ID
   */
  getById(id: string): Observable<ForumCategory> {
    return this.http.get<ForumCategory>(`${this.API_URL}/${id}`);
  }

  /**
   * Create a new forum category
   */
  create(request: CreateCategoryRequest): Observable<ForumCategory> {
    return this.http.post<ForumCategory>(this.API_URL, request);
  }

  /**
   * Update an existing forum category
   */
  update(id: string, request: UpdateCategoryRequest): Observable<ForumCategory> {
    return this.http.put<ForumCategory>(`${this.API_URL}/${id}`, request);
  }

  /**
   * Delete a forum category
   */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
