import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Message } from '../models/message.model';
@Component({
    standalone: true,
    selector: 'forum-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.scss'],
    imports: [DatePipe]
})
export class MessageComponent {
    message = input<Message>();
}
