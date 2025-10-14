import { Component, inject } from '@angular/core';
import { ForumService } from '../../services/forum.service';
import { FormsModule } from '@angular/forms';
import { AsyncPipe } from '@angular/common';
import { RouteButtonComponent } from '@/app/shared/components/backButton/back-button.component';
import { AuthService } from '@/app/core/auth/services/auth.service';
import { AuthUser } from '@/app/core/auth/models/auth.model';
import { Subject, takeUntil } from 'rxjs';
@Component({
    standalone: true,
    selector: 'forum-add-thread',
    templateUrl: './add-thread.component.html',
    styleUrls: ['./add-thread.component.scss'],
    imports: [FormsModule, AsyncPipe, RouteButtonComponent]
})

export class AddThreadComponent {
    title: string = '';
    content: string = '';
    categoryId: number = 0;

    error$: string = '';

    forumService: ForumService = inject(ForumService);
    authService = inject(AuthService);

    currentUser: AuthUser | null = null;
    private destroy$ = new Subject<void>();
    isLoading = true;

    categories$ = this.forumService.getCategories();

    ngOnInit(): void {
        this.loadUserData();
    }

    private loadUserData(): void {
        this.authService.currentUser$
            .pipe(takeUntil(this.destroy$))
            .subscribe({
                next: (user) => {
                    this.currentUser = user;
                    this.isLoading = false;
                },
                error: (error) => {
                    console.error('Error loading user data:', error);
                    this.isLoading = false;
                }
            });
    }

    createThread() {
        if (this.title && this.content && this.categoryId) {
            this.forumService.addThreadToCategory({
                title: this.title,
                content: this.content,
                categoryId: this.categoryId
            }).subscribe();
        }
        else {
            this.error$ = 'Veuillez remplir tous les champs';
        }
    }

}
