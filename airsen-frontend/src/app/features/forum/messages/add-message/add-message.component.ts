import { Component, input, inject, Output, Input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Thread } from '../../models/thread.model';
import { Message } from '../../models/message.model';
import { MessageService } from '../../services/message.service'
import { EventEmitter } from '@angular/core';
import { Observable } from 'rxjs';

@Component({
    selector: 'app-add-message',
    templateUrl: './add-message.component.html',
    styleUrls: ['./add-message.component.scss']
})
export class AddMessageComponent {
    thread = input<Thread | null | undefined>({
         id: 0,
        title: '',
        content: '',
        author: null,
        category: null,
        messages: [],
        votes: [],
        createdDate: new Date(),
        lastMessageDate: new Date(),
        viewCount: 0,
        likeCount: 0,
        closed: false,
        pinned: false,
        messageCount: 0
    },);
    content = '';

    @Output() newMessageEvent = new EventEmitter<number>();

    messageService = inject(MessageService);

    /**
     * Creates a new message with the given content for the thread with the given id.
     * Emits the newMessageEvent with the thread id when the message is successfully created.
     * If there is an error while creating the message, logs the error to the console.
     */
    createMessage() {
        if (this.thread && this.content) {
            this.messageService.addMessageToThread(this.thread()?.id, { content: this.content }).subscribe({
                next: (message) => {
                    this.content = '';
                    this.emitThreadInfos(this.thread()?.id);
                },
                error: (error) => {
                    console.error('Error creating message:', error);
                }
            });
        }
    }

/**
 * Emits the newMessageEvent with the thread id when a new message is successfully created for the thread.
 * This method is called after a new message is created for the thread.
 * @param threadId - the id of the thread for which a new message was created
 */
    emitThreadInfos(threadId: number | undefined) {
    this.newMessageEvent.emit(threadId);
  }
}
