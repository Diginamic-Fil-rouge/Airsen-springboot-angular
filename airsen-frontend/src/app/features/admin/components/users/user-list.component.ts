import { Component, OnInit } from '@angular/core';
import { AdminService, User, UserFilterParams, PaginatedResponse } from '../../services/admin.service';

/**
 * UserListComponent
 *
 * Displays paginated list of users with search and filter functionality.
 * Allows admins to suspend/activate users and update roles.
 */
@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
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

  constructor(private adminService: AdminService) {}

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
        this.error = 'Failed to load users. Please try again.';
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
    if (confirm('Are you sure you want to suspend this user?')) {
      const reason = prompt('Enter suspension reason:');
      if (reason) {
        this.adminService.suspendUser(userId, reason).subscribe({
          next: () => {
            alert('User suspended successfully');
            this.loadUsers();
          },
          error: (err) => {
            alert('Failed to suspend user');
            console.error(err);
          }
        });
      }
    }
  }

  activateUser(userId: number): void {
    if (confirm('Are you sure you want to activate this user?')) {
      this.adminService.activateUser(userId).subscribe({
        next: () => {
          alert('User activated successfully');
          this.loadUsers();
        },
        error: (err) => {
          alert('Failed to activate user');
          console.error(err);
        }
      });
    }
  }

  updateRole(userId: number, currentRole: string): void {
    const newRole = prompt(`Enter new role (current: ${currentRole}):`);
    if (newRole && newRole !== currentRole) {
      this.adminService.updateUserRole(userId, newRole).subscribe({
        next: () => {
          alert('User role updated successfully');
          this.loadUsers();
        },
        error: (err) => {
          alert('Failed to update user role');
          console.error(err);
        }
      });
    }
  }
}
