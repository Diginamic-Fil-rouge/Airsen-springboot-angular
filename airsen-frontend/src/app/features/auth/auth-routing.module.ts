import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';

/**
 * AuthRoutingModule - Routing configuration for authentication feature
 *
 * This module defines routes for user authentication flows:
 * - /auth → redirects to /auth/login
 * - /auth/login → LoginComponent (JWT authentication)
 * - /auth/register → RegisterComponent (new user registration)
 *
 * These routes are lazy-loaded from AppRoutingModule to reduce
 * initial bundle size. Authentication components are only loaded
 * when user navigates to auth routes.
 */
const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AuthRoutingModule { }
