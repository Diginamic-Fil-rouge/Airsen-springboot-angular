import { Component, input, inject, Output, Input } from '@angular/core';
import { Thread } from '../../models/thread.model';
import { MessageService } from '../../services/_message.service'
import { EventEmitter } from '@angular/core';

@Component({
    standalone : false,
    selector: 'app-add-message',
    templateUrl: './add-_message.component.html',
    styleUrls: ['./add-_message.component.scss']
})
export class AddMessageComponent {
    thread = input<Thread | null | undefined>({
         id: 0,
        title: '',
        content: '',
        author: null,
        category: null,
        _messages: [],
        votes: [],
        createdDate: new Date(),
        lastMessageDate: new Date(),
        viewCount: 0,
        likeCount: 0,
        closed: false,
        pinned: false,
        _messageCount: 0
    },);
    content = '';

    @Output() newMessageEvent = new EventEmitter<number>();

    _messageService = inject(MessageService);

    /**
     * Creates a new _message with the given content for the thread with the given id.
     * Emits the newMessageEvent with the thread id when the _message is successfully created.
     * If there is an error while creating the _message, logs the error to the console.
     */
    createMessage() {
        if (this.thread && this.content) {
            this._messageService.addMessageToThread(this.thread()?.id, { content: this.content }).subscribe({
                next: (_message) => {
                    this.content = '';
                    this.emitThreadInfos(this.thread()?.id);
                },
                error: (error) => {
                    console.error('Error creating _message:', error);
                }
            });
        }
    }

/**
 * Emits the newMessageEvent with the thread id when a new _message is successfully created for the thread.
 * This method is called after a new _message is created for the thread.
 * @param threadId - the id of the thread for which a new _message was created
 */
    emitThreadInfos(threadId: number | undefined) {
    this.newMessageEvent.emit(threadId);
  }
}
