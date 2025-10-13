import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

@Component({
  standalone: true,
  selector: 'route-button',
  templateUrl: './route-button.component.html',
  styleUrls: ['./route-button.component.scss'],
  imports: [RouterModule, CommonModule]
})

export class RouteButtonComponent {
   textContent = input<string>("");
   route = input<string>("");
}
