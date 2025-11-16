import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { CategoryService, ForumCategory, CreateCategoryRequest, UpdateCategoryRequest } from './category.service';

@Component({
  standalone:false,
  selector: 'app-category-list',
  templateUrl: './category-list.component.html',
  styleUrls: ['./category-list.component.scss']
})
export class CategoryListComponent implements OnInit, OnDestroy {
  categories: ForumCategory[] = [];
  loading = false;
  error: string | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private categoryService: CategoryService
  ) { }

  ngOnInit(): void {
    this.loadCategories();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all categories from the backend
   */
  loadCategories(): void {
    this.loading = true;
    this.error = null;

    this.categoryService.getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.categories = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Erreur lors du chargement des catégories:', err);
          this.error = 'Impossible de charger les catégories. Veuillez réessayer.';
          this.loading = false;
        }
      });
  }

  /**
   * Open dialog to create a new category
   */
  openAddDialog(): void {
    // Simple prompt-based dialog for MVP
    const name = prompt('Nom de la catégorie:');
    if (!name) return;

    const description = prompt('Description (min 10 caractères):');
    if (description === null) return;

    const color = prompt('Couleur (format hex, ex: #3B82F6):');
    if (!color) return;

    const request: CreateCategoryRequest = { name, description, color };

    this.categoryService.create(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadCategories();
        },
        error: (err) => {
          console.error('Erreur lors de la création:', err);
          alert('Impossible de créer la catégorie. Veuillez réessayer.');
        }
      });
  }

  /**
   * Open dialog to edit a category
   */
  openEditDialog(category: ForumCategory): void {
    const name = prompt('Nom de la catégorie:', category.name);
    if (!name) return;

    const description = prompt('Description:', category.description);
    if (description === null) return;

    const color = prompt('Couleur (format hex, ex: #3B82F6):', category.color || '#3B82F6');
    if (!color) return;

    const request: UpdateCategoryRequest = { name, description, color };

    this.categoryService.update(String(category.id), request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadCategories();
        },
        error: (err) => {
          console.error('Erreur lors de la modification:', err);
          alert('Impossible de modifier la catégorie. Veuillez réessayer.');
        }
      });
  }

  /**
   * Delete a category with confirmation
   */
  deleteCategory(category: ForumCategory): void {
    const confirmed = confirm(`Êtes-vous sûr de vouloir supprimer la catégorie "${category.name}" ?`);
    if (!confirmed) return;

    this.categoryService.delete(String(category.id))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadCategories();
        },
        error: (err) => {
          console.error('Erreur lors de la suppression:', err);
          alert('Impossible de supprimer la catégorie. Veuillez réessayer.');
        }
      });
  }
}
