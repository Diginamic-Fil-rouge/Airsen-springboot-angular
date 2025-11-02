import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '@/environments/environment';
import { Observable } from 'rxjs';
import { Thread } from '../models/thread.model';
import { Page } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class ThreadService {
  private readonly apiUrl = `${environment.apiUrl}/forum/threads`;
    private http = inject(HttpClient);
    threads: Observable<Thread[]> = this.http.get<Thread[]>(this.apiUrl);

    getAllThreads(): Observable<Page> {
        return this.http.get<Page>(`${this.apiUrl}`, { params: { sortBy: 'lastMessageDate' }});
    }

    getThreads(id: number): Observable<Thread[]> {
        if (id === 0) {
            return this.threads;
        }
        this.threads = this.http.get<Thread[]>(`${environment.apiUrl}/forum/categories/${id}/threads`);
        return this.threads;
    }

    getThread(id: number): Observable<Thread> {
        return this.http.get<Thread>(`${this.apiUrl}/${id}`);
    }

    editThread(thread: any): Observable<any> {
        return this.http.put(`${this.apiUrl}/${thread.id}`, thread);
    }

    deleteThread(id: number): Observable<any> {
        return this.http.delete(`${this.apiUrl}/${id}`);
    }
}
