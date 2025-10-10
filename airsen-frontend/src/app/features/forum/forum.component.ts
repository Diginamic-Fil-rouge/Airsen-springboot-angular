import { inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ForumService } from './services/forum.service';
import { ThreadService } from './services/thread.service';
import { CategoryComponent } from "./categories/category.component";
import { ThreadComponent } from './threads/thread.component';
import { Observable } from 'rxjs';
import { Thread } from './models/thread.model';
import { Page } from './models/page.model';

@Component({
  standalone: true,
  selector: 'app-forum',
  templateUrl: './forum.component.html',
  styleUrls: ['./forum.component.scss'],
  imports: [CategoryComponent, ThreadComponent, CommonModule]
})

export class ForumComponent {

  private forumService = inject(ForumService);
  private threadService = inject(ThreadService);
  categories$ = this.forumService.getCategories();
  allThreads$ = this.threadService.getAllThreads();
  threads$ = this.threadService.getThreads(1);

  getThreads(id: number) {
     this.allThreads$ = new Observable<Page>;
    this.threads$ = this.threadService.getThreads(id);

  }

  getAllThreads() {
    this.threads$ = new Observable<Thread[]>;
    this.allThreads$ = this.threadService.getAllThreads();
  }
}
