import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { QuickActionCard, QuickActionKey } from '../../models/quick-action';

@Component({
  selector: 'app-quick-actions',
  standalone: false,
  templateUrl: './quick-actions.html',
  styleUrls: ['./quick-actions.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickActionsComponent {
  @Input() actions: QuickActionCard[] = [];
  @Output() actionClick = new EventEmitter<QuickActionKey>();

  onActionClick(action: QuickActionKey): void {
    this.actionClick.emit(action);
  }
}