import { Component, inject } from '@angular/core';
import { ForumService } from '../../services/forum.service';
import { Router } from '@angular/router';
@Component({
    standalone : false,
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
    router = inject(Router);

    categories$ = this.forumService.getCategories();

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
