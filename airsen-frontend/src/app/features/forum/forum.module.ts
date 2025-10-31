import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@/shared/shared.module';

import { ForumRoutingModule } from './forum-routing.module';
import { ForumComponent } from './forum.component';
import { CategoryComponent } from './categories/category.component';
import { MessageComponent } from './messages/message.component';
import { ThreadComponent } from './threads/thread.component';
import { AddThreadComponent } from './threads/add-thread/add-thread.component';
import { EditThreadComponent } from './threads/edit-thread/edit-thread.component';
import { AddMessageComponent } from './messages/add-message/add-message.component';
import { ThreadDetailsComponent } from './threads/threads-details/thread-details.component';
import { VotingComponent } from './voting/voting.component';

/**
 * ForumModule - Lazy-loaded community forum feature module
 *
 * This module encapsulates the ecology forum feature where users discuss
 * environmental topics, share information, and engage with the community.
 * It is lazy-loaded to reduce initial bundle size.
 */
@NgModule({
  declarations: [
    ForumComponent,
    CategoryComponent,
    MessageComponent,
    ThreadComponent,
    AddThreadComponent,
    EditThreadComponent,
    AddMessageComponent,
    ThreadDetailsComponent,
    VotingComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    ForumRoutingModule
  ]
})
export class ForumModule { }
