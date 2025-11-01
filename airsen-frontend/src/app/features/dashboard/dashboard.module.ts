import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { DashboardRoutingModule } from "./dashboard-routing.module";
import { DashboardComponent } from "./dashboard.component";
import { QuickActionsComponent } from "./components/quick-actions/quick-actions";
import { AlertSummaryComponent } from "./components/alert-summary/alert-summary";
import { StatsPanelComponent } from "./components/stats-panel/stats-panel";
import { AirQualityWidgetComponent } from "./components/air-quality-widget/air-quality-widget";
import { SharedModule } from '@/shared/shared.module';

@NgModule({
  declarations: [
    DashboardComponent,
    QuickActionsComponent,
    AlertSummaryComponent,
    StatsPanelComponent,
    AirQualityWidgetComponent
  ],
  imports: [
    CommonModule,
    DashboardRoutingModule,
    SharedModule  // Provides Material modules, pipes, and shared components
  ],
})
export class DashboardModule {}