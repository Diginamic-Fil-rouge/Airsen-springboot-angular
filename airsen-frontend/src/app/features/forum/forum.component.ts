import { inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ForumService } from './services/forum.service';
import { CategoryComponent } from "./categories/category.component";

@Component({
  standalone: true,
  selector: 'app-forum',
  templateUrl: './forum.component.html',
  styleUrls: ['./forum.component.scss'],
  imports: [CategoryComponent, CommonModule]
})

export class ForumComponent {

  private forumService = inject(ForumService);
  categories$ = this.forumService.getCategories();

  constructor(private router: Router) {}

  goToHome(): void {
    this.router.navigate(['/home']);
  }
}
