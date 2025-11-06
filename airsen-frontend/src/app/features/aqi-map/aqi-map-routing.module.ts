import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AqiMapComponent } from './aqi-map.component';
import { AuthGuard } from '@/auth/guards/auth.guard';

const routes: Routes = [
  {
    path: '',
    component: AqiMapComponent,
    canActivate: [AuthGuard],
    data: {
      title: 'Carte de Qualité de l\'Air',
      breadcrumb: 'Carte AQI'
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AqiMapRoutingModule { }
