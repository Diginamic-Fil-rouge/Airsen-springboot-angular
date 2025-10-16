import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '@/environments/environment';
import { Observable } from 'rxjs';
import { Category } from '../models/category.model';

@Injectable({
  providedIn: 'root'
})
export class ForumService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/forum/categories`;
  private http = inject(HttpClient);
  categories: Observable<Category[]> = this.http.get<Category[]>(this.apiUrl);

  getCategories(): Observable<Category[]> {
    return this.categories;
  }

  addThreadToCategory(thread: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/${thread.categoryId}/threads`, thread);
  }
}