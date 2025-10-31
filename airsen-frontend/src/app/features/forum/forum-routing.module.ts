import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ForumComponent } from './forum.component';
import { ThreadDetailsComponent } from './threads/threads-details/thread-details.component';
import { AddThreadComponent } from './threads/add-thread/add-thread.component';
import { EditThreadComponent } from './threads/edit-thread/edit-thread.component';

/**
 * ForumRoutingModule - Routing configuration for forum feature
 *
 * This module defines routes for the community ecology forum:
 * - /forum → ForumComponent (categories list)
 * - /forum/thread/:id → ThreadDetailsComponent (thread with messages)
 * - /forum/thread/new → AddThreadComponent (create new thread)
 * - /forum/thread/:id/edit → EditThreadComponent (edit thread)
 *
 * The forum feature enables:
 * - Browse forum categories and threads
 * - View thread details with messages
 * - Create new threads and reply to messages
 * - Edit threads (author only)
 * - Vote on messages (upvote/downvote)
 *
 * Lazy loading configuration in AppRoutingModule:
 * {
 *   path: 'forum',
 *   loadChildren: () => import('./features/forum/forum.module').then(m => m.ForumModule),
 *   canActivate: [AuthGuard]
 * }
 */
const routes: Routes = [
  {
    path: '',
    component: ForumComponent
  },
  {
    path: 'thread/new',
    component: AddThreadComponent
  },
  {
    path: 'thread/:id',
    component: ThreadDetailsComponent
  },
  {
    path: 'thread/:id/edit',
    component: EditThreadComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ForumRoutingModule { }
