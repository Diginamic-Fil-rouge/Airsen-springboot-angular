import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Thread } from '../../models/thread.model';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { ThreadService } from '../../services/thread.service';
import { AsyncPipe, DatePipe } from '@angular/common';
import { MessageComponent } from '../../messages/message.component';
import { LoaderComponent } from '@/app/shared/components/loader/loader.component';
import { RouteButtonComponent } from '@/app/shared/components/backButton/back-button.component';
import { VotingComponent } from '../../voting/voting.component';

@Component({
    standalone: true,
    selector: 'forum-thread-details',
    templateUrl: './thread-details.component.html',
    styleUrls: ['./thread-details.component.scss'],
    imports: [AsyncPipe, DatePipe, RouterModule, CommonModule, MessageComponent, LoaderComponent, RouteButtonComponent, VotingComponent]
})
export class ThreadDetailsComponent {
    activatedRoute = inject(ActivatedRoute);
    service: ThreadService = inject(ThreadService)
    id = () => Number(this.activatedRoute.snapshot.params['id']);
    thread$!: Observable<Thread | undefined>;
    
    ngOnInit() {
        this.thread$ = this.service.getThread(this.id());
    }
}
