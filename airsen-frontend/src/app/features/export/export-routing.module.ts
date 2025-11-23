import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AuthGuard } from '@/core/auth/guards/auth.guard';
import { ExportPageComponent } from './pages/export/export.component';

const routes: Routes = [
  {
    path: '',
    component: ExportPageComponent,
    canActivate: [AuthGuard],
    data: { title: 'Mes Exports' }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ExportRoutingModule { }