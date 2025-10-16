import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '@/environments/environment';
import { Observable } from 'rxjs';
import { Message } from '../models/message.model';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/forum/threads`;
    private http = inject(HttpClient);
    messages: Observable<Message[]> = new Observable<Message[]>();

    getMessagesByThread(threadId: number): Observable<Message[]> {
        this.messages = this.http.get<Message[]>(`${this.apiUrl}/${threadId}/messages`);
        return this.messages;
    }

    addMessageToThread(threadId: number | undefined, message: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/${threadId}/messages`, message);
    }

    editMessage(message: any): Observable<any> {
      return this.http.put(`${environment.apiUrl}/api/v1/forum/messages/${message?.id}`, message);
    }
}