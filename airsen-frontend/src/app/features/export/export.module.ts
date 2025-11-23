import { NgModule } from '@angular/core';

import { ExportRoutingModule } from './export-routing.module';
import { ExportPageComponent } from './pages/export/export.component';
import { CsvExportPanelComponent } from './components/csv-export-panel/csv-export-panel.component';
import { ExportHistoryComponent } from './components/export-history/export-history.component';

import { SharedModule } from '@/shared/shared.module';

@NgModule({
  declarations: [
    ExportPageComponent,
    CsvExportPanelComponent,
    ExportHistoryComponent
  ],
  imports: [
    SharedModule,
    ExportRoutingModule
  ]
})
export class ExportModule { }