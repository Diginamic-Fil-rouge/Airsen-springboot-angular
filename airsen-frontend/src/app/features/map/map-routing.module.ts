import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MapComponent } from './map.component';
import { AuthGuard } from '@/auth/guards/auth.guard';

const routes: Routes = [
  {
    path: '',
    component: MapComponent,
    canActivate: [AuthGuard],
    data: {
      title: 'Carte de Qualité de l\'Air',
      breadcrumb: 'Carte'
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class MapRoutingModule { }
