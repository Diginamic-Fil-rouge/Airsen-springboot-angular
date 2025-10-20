import { Component, EventEmitter, inject, input, Output } from '@angular/core';
import { Message } from '../models/message.model';
import { MessageService } from '../services/message.service';
import { AuthService } from '@/app/core/auth/services/auth.service';
@Component({
    standalone: false,
    selector: 'forum-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.scss']
})
export class MessageComponent {
    service = inject(MessageService);
    authService = inject(AuthService);
    message = input<Message>();

    currentUser = this.authService.getCurrentUser();

    showModal = false;
    isEditing = false;

    content: string | undefined = '';

    @Output() messageEvent = new EventEmitter<number>();

    ngOnInit() {
        this.content = this.message()?.content;
    }

    triggerEdit() {
        this.isEditing = !this.isEditing
    }

    editMessage() {
        this.service.editMessage({ content: this.content, id: this.message()?.id }).subscribe({
            next: () => {
                this.emitMessageEvent();
                this.isEditing = false;
            },
            error: (error) => {
                console.error('Error editing message:', error);
            }
        });
    }

    emitMessageEvent() {
        this.messageEvent.emit(this.message()?.thread?.id);
    }

    toggleModal() {
        this.showModal = !this.showModal;
    }

    deleteMessage() {
        const id = this.message()?.id;
        if (id == null) {
            console.error('Cannot delete message: id is undefined or null.');
            return;
        }
        return this.service.deleteMessage(this.message()?.id).subscribe({
            next: () => {
                this.emitMessageEvent();
                this.showModal = false;
            },
            error: (error) => {
                console.error('Error deleting thread:', error);
            }
        });
    }

}
