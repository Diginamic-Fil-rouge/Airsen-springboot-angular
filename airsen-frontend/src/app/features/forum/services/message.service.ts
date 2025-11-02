import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { environment } from '@/environments/environment';
import { Observable } from 'rxjs';
import { Message } from '../models/message.model';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private readonly apiUrl = `${environment.apiUrl}/forum/threads`;
  private http = inject(HttpClient);
  messages: Observable<Message[]> = new Observable<Message[]>();

  getMessagesByThread(threadId: number): Observable<Message[]> {
    this.messages = this.http.get<Message[]>(`${this.apiUrl}/${threadId}/messages`);
    return this.messages;
  }

  addMessageToThread(threadId: number | undefined, message: any): Observable<any> {
    if (typeof threadId !== 'number' || isNaN(threadId)) {
      throw new Error('addMessageToThread: threadId must be a valid number');
    }
    return this.http.post(`${this.apiUrl}/${threadId}/messages`, message);
  }

  deleteMessage(messageId: number | undefined): Observable<void> {
    if (typeof messageId !== 'number' || isNaN(messageId)) {
      throw new Error('deleteMessage: messageId must be a valid number');
    }
    return this.http.delete<void>(`${environment.apiUrl}/forum/messages/${messageId}`);
  }

  editMessage(message: any): Observable<any> {
    return this.http.put(`${environment.apiUrl}/forum/messages/${message?.id}`, message);
  }
}
