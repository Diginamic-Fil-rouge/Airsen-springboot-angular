import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '@/environments/environment';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
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

    /**
     * Gets threads created by a specific user.
     * If backend doesn't support authorId parameter, fetches all and filters client-side.
     */
    getThreadsByAuthor(authorId: number): Observable<Thread[]> {
        // Try backend endpoint with authorId parameter
        return this.http.get<Page>(`${this.apiUrl}`, {
            params: {
                authorId: authorId.toString(),
                size: '1000' // Large size to get all user threads
            }
        }).pipe(
            map((page: Page) => page.content || []),
            catchError(() => {
                // Fallback: fetch all threads and filter client-side
                return this.getAllThreads().pipe(
                    map((page: Page) => {
                        if (page && page.content && Array.isArray(page.content)) {
                            return page.content.filter(
                                (thread: Thread) => thread.author && thread.author.id === authorId
                            );
                        }
                        return [];
                    })
                );
            })
        );
    }
}
