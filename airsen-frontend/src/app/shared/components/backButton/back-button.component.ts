import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

@Component({
  standalone: true,
  selector: 'back-button',
  templateUrl: './back-button.component.html',
  styleUrls: ['./back-button.component.scss'],
  imports: [RouterModule, CommonModule]
})

export class RouteButtonComponent {
   textContent = input<string>("");
   route = input<string>("");
}
