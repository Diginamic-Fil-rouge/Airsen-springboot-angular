import { Component, inject, OnInit } from '@angular/core';
import { Thread } from '../../models/thread.model';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { ThreadService } from '../../services/thread.service';
import { Message } from '../../models/message.model';
import { AuthService } from '@/app/core/auth/services/auth.service';
@Component({
  standalone: false,
  selector: 'app-forum-thread-details',
  templateUrl: './thread-details.component.html',
  styleUrls: ['./thread-details.component.scss']
})
export class ThreadDetailsComponent implements OnInit {
  activatedRoute = inject(ActivatedRoute);
  service: ThreadService = inject(ThreadService);
  authService = inject(AuthService);
  router = inject(Router);

  id = () => Number(this.activatedRoute.snapshot.params['id']);
  currentUser = this.authService.getCurrentUser();
  thread?: Thread;
  isLoading = true;

  showModal = false;

  ngOnInit() {
    this.loadThread();
  }

  /**
   * Loads the thread with the given id from the backend.
   * Sets the isLoading property to true while the thread is being loaded.
   * Sets the thread property to the loaded thread, and the isLoading property to false when the thread is successfully loaded.
   * Logs an error to the console if there is an error while loading the thread.
   */
  loadThread() {
    this.isLoading = true;
    this.service.getThread(this.id()).subscribe({
      next: (thread) => {
        this.thread = thread;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading thread:', error);
        this.isLoading = false;
      }
    });
  }


  /**
   * Reloads the thread details.
   * This method is useful when the thread details need to be refreshed,
   * for example after a message has been deleted.
   */
  refreshThread() {
    this.loadThread();
  }

  /**
   * Toggles the showModal property to show or hide the confirmation modal for deleting a thread.
   */
  toggleModal() {
    this.showModal = !this.showModal;
  }

  /**
   * Deletes the thread with the given id.
   * Hides the confirmation modal after deletion and navigates to the forum page.
   */
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
