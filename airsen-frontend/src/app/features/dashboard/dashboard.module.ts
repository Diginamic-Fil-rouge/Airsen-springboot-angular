import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { DashboardRoutingModule } from "./dashboard-routing.module";
import { DashboardComponent } from "./dashboard.component";
import { QuickActionsComponent } from "./components/quick-actions/quick-actions";
import { AlertSummaryComponent } from "./components/alert-summary/alert-summary";
import { StatsPanelComponent } from "./components/stats-panel/stats-panel";
import { SharedModule } from '@/shared/shared.module';
import { BaseChartDirective } from "ng2-charts";
import { AirQualityChartComponent } from "./components/air-quality-chart/air-quality-chart.component";

@NgModule({
  declarations: [
    DashboardComponent,
    QuickActionsComponent,
    AlertSummaryComponent,
    StatsPanelComponent,
    AirQualityChartComponent
  ],
  imports: [
    CommonModule,
    DashboardRoutingModule,
    SharedModule,
    BaseChartDirective
  ],
  exports: [
    AirQualityChartComponent
  ]
})
export class DashboardModule {}
