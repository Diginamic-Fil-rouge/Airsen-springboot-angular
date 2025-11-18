import { Component, OnInit } from '@angular/core';
import { AdminService, User, UserFilterParams, PaginatedResponse } from '../../services/admin.service';
import { ToastService } from '@/shared/services/toast.service';

/**
 * UserListComponent
 *
 * Displays paginated list of users with search and filter functionality.
 * Allows admins to suspend/activate users and update roles.
 */
@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss'],
  standalone: false
})
export class UserListComponent implements OnInit {
  users: User[] = [];
  loading = true;
  error: string | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Filters
  searchTerm = '';
  roleFilter = '';
  statusFilter: boolean | undefined = undefined;

  // Dialogs
  showSuspendConfirm = false;
  showActivateConfirm = false;
  showSuspendInput = false;
  selectedUserId: number | null = null;
  suspensionReason = '';

  constructor(
    private adminService: AdminService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = null;

    const params: UserFilterParams = {
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm || undefined,
      role: this.roleFilter || undefined,
      isActive: this.statusFilter
    };

    this.adminService.getUsers(params).subscribe({
      next: (response: PaginatedResponse<User>) => {
        this.users = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Échec du chargement des utilisateurs. Veuillez réessayer.';
        this.loading = false;
        console.error('Error loading users:', err);
      }
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadUsers();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadUsers();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadUsers();
  }

  suspendUser(userId: number): void {
    this.selectedUserId = userId;
    this.showSuspendConfirm = true;
  }

  onSuspendConfirmed(): void {
    this.showSuspendInput = true;
  }

  onSuspendReasonSubmitted(reason: string): void {
    if (this.selectedUserId !== null) {
      this.adminService.suspendUser(this.selectedUserId, reason).subscribe({
        next: () => {
          this.toastService.success('Utilisateur suspendu avec succès');
          this.loadUsers();
          this.selectedUserId = null;
        },
        error: (err) => {
          this.toastService.error('Échec de la suspension de l\'utilisateur');
          console.error(err);
        }
      });
    }
  }

  activateUser(userId: number): void {
    this.selectedUserId = userId;
    this.showActivateConfirm = true;
  }

  onActivateConfirmed(): void {
    if (this.selectedUserId !== null) {
      this.adminService.activateUser(this.selectedUserId).subscribe({
        next: () => {
          this.toastService.success('Utilisateur activé avec succès');
          this.loadUsers();
          this.selectedUserId = null;
        },
        error: (err) => {
          this.toastService.error('Échec de l\'activation de l\'utilisateur');
          console.error(err);
        }
      });
    }
  }

  updateRole(userId: number, currentRole: string): void {
    // For now, keep using prompt for role update - can be enhanced with a select dialog later
    const newRole = prompt(`Entrer le nouveau rôle (actuel : ${currentRole}) :`);
    if (newRole && newRole !== currentRole) {
      this.adminService.updateUserRole(userId, newRole).subscribe({
        next: () => {
          this.toastService.success('Rôle de l\'utilisateur mis à jour avec succès');
          this.loadUsers();
        },
        error: (err) => {
          this.toastService.error('Échec de la mise à jour du rôle de l\'utilisateur');
          console.error(err);
        }
      });
    }
  }
}
