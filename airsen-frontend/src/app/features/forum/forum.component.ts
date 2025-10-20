import { inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ForumService } from './services/forum.service';
import { ThreadService } from './services/thread.service';
import { Observable } from 'rxjs';
import { Thread } from './models/thread.model';
import { Page } from './models/page.model';
import { LoaderComponent } from '@/app/shared/components/loader/loader.component';
import { AuthService } from '@/app/core/auth/services/auth.service';

@Component({
  standalone : false,
  selector: 'app-forum',
  templateUrl: './forum.component.html',
  styleUrls: ['./forum.component.scss']
})

export class ForumComponent {

  private forumService = inject(ForumService);
  private threadService = inject(ThreadService);
  private authService = inject(AuthService);

  currentUser = this.authService.getCurrentUser();
  categories$ = this.forumService.getCategories();
  allThreads$ = this.threadService.getAllThreads();
  threads$ = this.threadService.getThreads(1);

/**
 * Retrieves the threads for the given category id.
 * @param id - the id of the category to retrieve the threads for.
 * @returns An observable of the threads for the given category id.
 */
  getThreads(id: number) {
     this.allThreads$ = new Observable<Page>;
    this.threads$ = this.threadService.getThreads(id);

  }

/**
 * Retrieves all the threads from the server.
 * @returns An observable of all the threads.
 */
  getAllThreads() {
    this.threads$ = new Observable<Thread[]>;
    this.allThreads$ = this.threadService.getAllThreads();
  }

}
