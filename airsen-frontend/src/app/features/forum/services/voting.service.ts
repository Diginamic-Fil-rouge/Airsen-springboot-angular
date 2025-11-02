import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '@/environments/environment';
import { Observable } from 'rxjs';
@Injectable({
  providedIn: 'root'
})
export class VotingService {
  private readonly apiUrl = `${environment.apiUrl}/forum/threads`;
    private http = inject(HttpClient);

    voteThread(threadId: number | undefined, voteType: string): Observable<any> {
        return this.http.post(`${this.apiUrl}/${threadId}/vote`, { voteType: voteType });
    }

    unvoteThread(threadId: number | undefined) : Observable<any> {
        return this.http.delete(`${this.apiUrl}/${threadId}/vote`);
    }
}
