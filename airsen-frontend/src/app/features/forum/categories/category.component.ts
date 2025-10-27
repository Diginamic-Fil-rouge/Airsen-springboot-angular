import { Component, input } from '@angular/core';
import { Category } from '../models/category.model';

@Component({
  standalone : false,
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
}
