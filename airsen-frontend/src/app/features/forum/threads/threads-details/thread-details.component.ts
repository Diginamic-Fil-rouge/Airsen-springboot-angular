import { Component, inject } from '@angular/core';
import { Thread } from '../../models/thread.model';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { ThreadService } from '../../services/thread.service';
import { Message } from '../../models/message.model';
import { AuthService } from '@/app/core/auth/services/auth.service';
@Component({
    standalone: false,
    selector: 'forum-thread-details',
    templateUrl: './thread-details.component.html',
    styleUrls: ['./thread-details.component.scss']
})
export class ThreadDetailsComponent {
    activatedRoute = inject(ActivatedRoute);
    service: ThreadService = inject(ThreadService);
    authService = inject(AuthService);
    router = inject(Router);

    id = () => Number(this.activatedRoute.snapshot.params['id']);
    currentUser = this.authService.getCurrentUser();
    thread$!: Observable<Thread | undefined>;

    showModal = false;
    
    ngOnInit() {
        this.thread$ = this.service.getThread(this.id());
    }

/**
 * Refreshes the thread data by fetching the thread using the current route's id.
 * @param threadId - the id of the thread to refresh
 */
    refreshThread() {
    this.thread$ = this.service.getThread(this.id());
  }

  displayModal() {
    this.showModal = !this.showModal;
  }

  deleteThread() {
    return this.service.deleteThread(this.id()).subscribe({
      next: () => {
        this.showModal = false;
        this.router.navigate(['/forum/']);
      },
      error: (error) => {
        console.error('Error deleting thread:', error);
      }
    });
  }
}
