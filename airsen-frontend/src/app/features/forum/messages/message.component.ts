import { Component, input } from '@angular/core';
import { Message } from '../models/message.model';
@Component({
    standalone : false,
    selector: 'forum-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.scss']
})
export class MessageComponent {
    message = input<Message>();
}
