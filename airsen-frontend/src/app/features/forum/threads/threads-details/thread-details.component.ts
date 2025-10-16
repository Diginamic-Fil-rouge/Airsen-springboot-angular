import { Component, inject } from '@angular/core';
import { Thread } from '../../models/thread.model';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { ThreadService } from '../../services/thread.service';
import { Message } from '../../models/message.model';

@Component({
    standalone: false,
    selector: 'forum-thread-details',
    templateUrl: './thread-details.component.html',
    styleUrls: ['./thread-details.component.scss']
})
export class ThreadDetailsComponent {
    activatedRoute = inject(ActivatedRoute);
    service: ThreadService = inject(ThreadService)
    id = () => Number(this.activatedRoute.snapshot.params['id']);
    thread$!: Observable<Thread | undefined>;
    
    ngOnInit() {
        this.thread$ = this.service.getThread(this.id());
    }

/**
 * Refreshes the thread data by getting the thread with the given id
 * @param threadId - the id of the thread to refresh
 */
    refreshThread(threadId: number) {
    this.thread$ = this.service.getThread(threadId);
  }
}
