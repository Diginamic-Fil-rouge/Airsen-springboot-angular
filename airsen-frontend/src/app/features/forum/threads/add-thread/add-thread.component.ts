import { Component, inject } from '@angular/core';
import { ForumService } from '../../services/forum.service';
import { AuthService } from '@/app/core/auth/services/auth.service';
import { AuthUser } from '@/app/core/auth/models/auth.model';
import { Subject, takeUntil } from 'rxjs';
import { Router } from '@angular/router';
@Component({
    selector: 'forum-add-thread',
    templateUrl: './add-thread.component.html',
    styleUrls: ['./add-thread.component.scss']
})

export class AddThreadComponent {
    title: string = '';
    content: string = '';
    categoryId: number = 0;

    errors$: string[] = [];

    forumService: ForumService = inject(ForumService);
    authService = inject(AuthService);
    router = inject(Router);

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
        this.errors$ = [];
        if (this.title && this.content && this.categoryId) {
            this.forumService.addThreadToCategory({
                title: this.title,
                content: this.content,
                categoryId: this.categoryId
            }).subscribe({
                next: () => {
                    this.router.navigate(['/forum']);
                },
                error: (error) => {
                    console.error('Error creating thread:', error);
                    this.errors$.push('Erreur lors de la création du thread : ' + error.error.message);
                    for (const e of error.error.details){
                        this.errors$.push(e.message);
                    }
                }
            });
        }
        else {
            this.errors$.push('Veuillez remplir tous les champs');
        }
    }

}
