import { Component } from '@angular/core';

/**
 * SearchBarComponent
 *
 * Purpose: Autocomplete search input for communes. Emits user queries and
 * selections to the parent sidebar. Integrates with Angular Material form
 * field and autocomplete for a consistent look and feel.
 */
@Component({
  standalone: false,
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.scss']
})
export class SearchBarComponent {}

