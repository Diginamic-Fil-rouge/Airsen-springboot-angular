import { Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Message } from '../models/message.model';
@Component({
    standalone: false,
    selector: 'forum-message',
    templateUrl: './message.component.html',
    styleUrls: ['./message.component.scss'],
    imports: [DatePipe]
})
export class AddMessageComponent {
    message = input<Message>();
}
