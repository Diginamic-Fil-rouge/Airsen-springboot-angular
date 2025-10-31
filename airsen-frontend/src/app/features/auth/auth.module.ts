import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@/shared/shared.module';

import { AuthRoutingModule } from './auth-routing.module';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';

/**
 * AuthModule - Lazy-loaded authentication feature module
 *
 * This module encapsulates all authentication-related components and routing.
 * It is lazy-loaded to reduce initial application bundle size and improve
 * performance. The module is only loaded when users navigate to auth routes.
 *
 * Components:
 * - LoginComponent: User login with email/password (JWT authentication)
 * - RegisterComponent: New user registration with validation
 *
 * Dependencies:
 * - SharedModule: Provides Material components, forms, shared utilities
 * - AuthRoutingModule: Routes for /auth/login and /auth/register
 *
 * Services:
 * - AuthService: Provided by CoreModule (singleton, injected from root)
 */
@NgModule({
  declarations: [
    LoginComponent,
    RegisterComponent
  ],
  imports: [
    CommonModule,
    SharedModule,      // Material, Forms, shared components
    AuthRoutingModule  // Auth-specific routes
  ]
})
export class AuthModule { }
