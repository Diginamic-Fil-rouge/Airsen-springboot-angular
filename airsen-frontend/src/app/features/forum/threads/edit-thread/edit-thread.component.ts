import { Component, inject } from '@angular/core';
import { ForumService } from '../../services/forum.service';
import { ThreadService } from '../../services/thread.service';
import { Observable } from 'rxjs';
import { Router, ActivatedRoute } from '@angular/router';
import { Thread } from '../../models/thread.model';
import { Category } from '../../models/category.model';
@Component({
    standalone: false,
    selector: 'forum-edit-thread',
    templateUrl: './edit-thread.component.html',
    styleUrls: ['./edit-thread.component.scss']
})

export class EditThreadComponent {
    activatedRoute = inject(ActivatedRoute);
    service: ThreadService = inject(ThreadService)
    forumService: ForumService = inject(ForumService);
    router = inject(Router);

    id = () => Number(this.activatedRoute.snapshot.params['id']);
    thread$!: Observable<Thread | undefined>;
    categories$!: Observable<Category[] | undefined>;

    title: string = '';
    content: string = '';
    categoryId: number | undefined = 0;

    errors$: string[] = [];

/**
 * Initializes the component by retrieving the thread with the given id and categories.
 * Subscribes to the thread observable and sets the component's title, content and categoryId properties
 * if the thread is not null.
 */
    ngOnInit() {
        this.thread$ = this.service.getThread(this.id());
        this.categories$ = this.forumService.getCategories();
        this.thread$.subscribe(thread => {
            if (thread) {
                this.title = thread.title;
                this.content = thread.content;
                this.categoryId = thread.category?.id;
            }
        });
    }

    /**
     * Updates the thread with the given id with the new title, content and category id.
     * If the thread is successfully updated, navigates to the thread page.
     * If there is an error while updating the thread, logs the error to the console and updates the errors array with the error message and details.
     */
    updateThread() {
        this.errors$ = [];
        if (this.title && this.content && this.categoryId) {
            this.service.editThread({
                id: this.id(),
                title: this.title,
                content: this.content,
                categoryId: this.categoryId
            }).subscribe({
                next: () => {
                    this.router.navigate(['/forum/thread', this.id()]);
                },
                error: (error) => {
                    console.error('Error editing thread:', error);
                    this.errors$.push('Erreur lors de la modification du thread : ' + error.error.message);
                    for (const e of error.error.details) {
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
