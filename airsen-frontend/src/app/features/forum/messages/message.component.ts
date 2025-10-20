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

    /**
     * Toggles the isEditing property to show or hide the edit form.
     * @returns void
     */
    triggerEdit() {
        this.isEditing = !this.isEditing
    }

    /**
     * Edits the message with the given id and content.
     * If the message is successfully edited, emits the messageEvent with the thread id and sets the isEditing property to false.
     * If there is an error while editing the message, logs the error to the console.
     * @returns void
     */
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

    /**
     * Emits the messageEvent with the thread id when a message is edited or deleted.
     * @returns void
     */
    emitMessageEvent() {
        this.messageEvent.emit(this.message()?.thread?.id);
    }

    /**
     * Toggles the showModal property to show or hide the confirmation modal for deleting a message.
     */
    toggleModal() {
        this.showModal = !this.showModal;
    }

    /**
     * Deletes the message with the given id.
     * If the message is successfully deleted, emits the messageEvent with the thread id and sets the showModal property to false.
     * If there is an error while deleting the message, logs the error to the console.
     * @returns An Observable that contains the result of the delete operation.
     */
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
