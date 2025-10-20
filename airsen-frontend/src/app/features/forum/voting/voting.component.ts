import { Component, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VotingService } from '../services/voting.service';
import { Thread } from '../models/thread.model';
import { ThreadService } from '../services/thread.service';
import { AuthService } from '@/app/core/auth/services/auth.service';
import { AuthUser } from '@/app/core/auth/models/auth.model';
import { Vote } from '../models/vote.model';

@Component({
    standalone: false,
    selector: 'thread-voting',
    templateUrl: './voting.component.html',
    styleUrls: ['./voting.component.scss']
})
export class VotingComponent {

    thread = input<Thread | null | undefined>(undefined);
    voteCount = input<number | undefined>(0);
    counter: number | undefined = 0;

    hasVoted = false;
    thumbDownChecked = false;
    thumbUpChecked = false;

    votingService = inject(VotingService);
    threadService = inject(ThreadService);
    authService = inject(AuthService);

    user: AuthUser | null = this.authService.getCurrentUser();
    currentUserVote: Vote | null = null;

    /**
     * Lifecycle hook that is called after the component's input properties have been checked.
     * Updates the vote count and checks if the current user has voted for the thread.
     * If the user has voted, sets the currentUserVote and hasVoted properties accordingly.
     */
    ngOnChanges() {
        this.counter = this.voteCount();
        for (const vote of this.thread()?.votes ?? []) {
            if (vote?.user?.id === this.user?.id) {
                this.currentUserVote = vote;
                this.hasVoted = true;
                this.updateThumbsRender(vote.voteType);
                break;
            }
        }
    }

    /**
     * Updates the thumbs render according to the vote type.
     * If the vote type is 'LIKE', the thumb up is checked and the thumb down is unchecked.
     * If the vote type is 'DISLIKE', the thumb down is checked and the thumb up is unchecked.
     * If the vote type is neither 'LIKE' nor 'DISLIKE', both thumbs are unchecked.
     * @param voteType - The type of the vote ('LIKE' or 'DISLIKE')
     */
    updateThumbsRender(voteType: string) {
        if (voteType === 'LIKE') {
            this.thumbDownChecked = false;
            this.thumbUpChecked = true;
        }
        else if (voteType === 'DISLIKE') {
            this.thumbDownChecked = true;
            this.thumbUpChecked = false;

        }
        else {
            this.thumbDownChecked = false;
            this.thumbUpChecked = false;
        }
    }

    /**
     * Toggle vote for a thread.
     * If the user has already voted, unvote the thread.
     * If the user has not voted, vote for the thread with the given vote type.
     * @param voteType - the type of the vote (LIKE or DISLIKE)
     */
    voteSignal(voteType: string) {
        if (this.hasVoted) {
            this.unvoteThread(this.thread()?.id, voteType);
        } else {
            this.voteThread(this.thread()?.id, voteType);
        }
    }
    
    /**
     * Vote for a thread with the given vote type.
     * @param threadId - the id of the thread to vote
     * @param voteType - the type of the vote (LIKE or DISLIKE)
    */
   voteThread(threadId: number | undefined, voteType: string): void {
       if (threadId === undefined) return;
       this.votingService.voteThread(threadId, voteType).subscribe(
           {
               next: (res) => {
                   console.log("thread new vote count : " + res.likeCount);
                   this.counter = res.likeCount;
                   for (const vote of res.votes ?? []) {
                       if (vote?.user?.id === this.user?.id) {
                           this.currentUserVote = vote;
                           break;
                        }
                    }
                    this.updateThumbsRender(voteType);
                    this.hasVoted = true;
                },
                error: (error) => {
                    console.error('Error voting thread:', error);
                }
            }
        );
    }
    
    /**
     * Unvote a thread, if the user has already voted.
     * Reset the UI by setting the vote count, and updating the thumbs render.
     * @param threadId - the id of the thread to unvote
    */
   unvoteThread(threadId: number | undefined, voteType: string): void {
       if (threadId === undefined) return;
       this.votingService.unvoteThread(threadId).subscribe(
           {
               next: () => {
                   this.updateThumbsRender('UNVOTE');
                   this.counter = this.counter ? this.counter + (this.currentUserVote?.voteType === 'LIKE' ? -1 : 1) : 0;
                   if (this.currentUserVote?.voteType !== voteType){
                     this.voteThread(threadId, voteType);
                   }
                   this.currentUserVote = null;
                   this.hasVoted = false;
                },
                error: (error) => {
                    console.error('Error unvoting thread:', error);
                }
            }
        );
    }
    
}
