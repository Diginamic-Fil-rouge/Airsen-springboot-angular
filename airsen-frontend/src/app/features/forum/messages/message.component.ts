import { Component, input, inject } from '@angular/core';
import { Message } from '../models/message.model';
import { MessageService } from '../services/message.service';

@Component({
    standalone: false,
    selector: 'forum-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.scss']
})
export class MessageComponent {
    message = input<Message>();
    service = inject(MessageService);
    showModal = false;

    displayModal() {
        this.showModal = !this.showModal;
    }

    deleteMessage() {
        return this.service.deleteMessage(this.message()?.id).subscribe({
            next: () => {
                // This currently does not work, but should once the edit_message branch is merged
                // TODO : rename the EventEmitter simply to messageEvent (currently : editMessageEvent)
                this.emitEditMessageEvent();
                this.showModal = false;
            },
            error: (error) => {
                console.error('Error creating thread:', error);
            }
        });
    }
}
