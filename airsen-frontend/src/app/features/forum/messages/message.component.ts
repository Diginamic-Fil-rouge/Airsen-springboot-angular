import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Message } from '../models/message.model';
@Component({
    selector: 'forum-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.scss']
})
export class MessageComponent {
    message = input<Message>();
}
