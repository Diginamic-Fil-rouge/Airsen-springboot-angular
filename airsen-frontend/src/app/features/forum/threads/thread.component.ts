import { Component, input } from '@angular/core';
import { Thread } from '../models/thread.model';
import { Router } from '@angular/router';
@Component({
    standalone : false,
    selector: 'forum-thread',
    templateUrl: './thread.component.html',
    styleUrls: ['./thread.component.scss']
})
export class ThreadComponent {

    thread = input<Thread>({
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
    constructor(private router: Router) { }
    goToThread(id: number): void {
        this.router.navigate([`/forum/thread/${id}`]);
    }
}
