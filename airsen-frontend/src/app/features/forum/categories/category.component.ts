import { Component, input } from '@angular/core';
import { Router } from '@angular/router';
import { Category } from '../models/category.model';

@Component({
  standalone: true,
  selector: 'forum-category',
  templateUrl: './category.component.html',
  styleUrls: ['./category.component.scss']
})
export class CategoryComponent {

  category = input<Category>({
     id: 0,
      name: "",
      description: "",
      color: "",
      threads: []
  },);

  constructor(private router: Router) {}

  applyCategory(): void {
    // this.router.navigate(['/forum']);
  }
}
