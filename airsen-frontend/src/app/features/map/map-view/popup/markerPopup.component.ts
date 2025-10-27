import { Component, Output, EventEmitter, Input } from '@angular/core';
import { Commune } from '../../models/commune.model';
@Component({
  standalone: false,
  selector: 'app-marker-popup',
  templateUrl: './markerPopup.component.html',
  styleUrls: ['./markerPopup.component.scss']
})
export class MarkerPopupComponent {
  @Input() commune: Commune | null = null;
  @Output() more = new EventEmitter<void>();

  onMore() {
    this.more.emit();
  }
}

