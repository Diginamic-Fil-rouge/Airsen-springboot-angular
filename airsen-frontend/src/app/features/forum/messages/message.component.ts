import { Component, EventEmitter, inject, input, Output } from '@angular/core';
import { Message } from '../models/message.model';
import { MessageService } from '../services/message.service';

@Component({
    selector: 'forum-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.scss']
})
export class MessageComponent {
    messageService = inject(MessageService);
    message = input<Message>();

    isEditing = false;

    content: string | undefined = '';

    @Output() editMessageEvent = new EventEmitter<number>();

    ngOnInit() {
        this.content = this.message()?.content;
    }

    triggerEdit() {
        this.isEditing = !this.isEditing
    }

    editMessage() {
        this.messageService.editMessage({ content: this.content, id: this.message()?.id }).subscribe({
            next: () => {
                this.emitEditMessageEvent();
                this.isEditing = false;
            },
            error: (error) => {
                console.error('Error editing message:', error);
            }
        });
    }

    emitEditMessageEvent() {
        this.editMessageEvent.emit(this.message()?.thread?.id);
    }
}
