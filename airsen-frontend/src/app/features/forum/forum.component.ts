import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  standalone: false,
  selector: 'app-forum',
  templateUrl: './forum.component.html',
  styleUrls: ['./forum.component.scss']
})
export class ForumComponent {
  constructor(private router: Router) {}

  goToHome(): void {
    this.router.navigate(['/home']);
  }
}
