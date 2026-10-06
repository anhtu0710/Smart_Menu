import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  authService = inject(AuthService);
  router = inject(Router);
  currentUser = this.authService.currentUser;
  
  showUserDropdown = signal(false);

  toggleDropdown() {
    this.showUserDropdown.update(val => !val);
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }

  logout() {
    this.authService.logout();
    this.showUserDropdown.set(false);
    this.router.navigate(['/']);
  }
}
