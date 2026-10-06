import { Component, inject, signal, OnInit, OnDestroy, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {
  authService = inject(AuthService);
  router = inject(Router);
  currentUser = this.authService.currentUser;
  platformId = inject(PLATFORM_ID);

  // Trạng thái mẫu menu được chọn (gold, emerald, rustic)
  selectedStyle = signal<'gold' | 'emerald' | 'rustic'>('gold');

  // Danh sách các mẫu menu ở Hero banner
  heroMenus = [
    { id: 'modern', name: 'Coffee Hiện Đại', image: '/images/coffee_modern_bg.png' },
    { id: 'luxury', name: 'Coffee Sang Trọng', image: '/images/coffee_luxury_bg.png' },
    { id: 'traditional', name: 'Coffee Truyền Thống', image: '/images/coffee_traditional_bg.png' },
    { id: 'youthful', name: 'Coffee Trẻ Trung', image: '/images/coffee_youthful_bg.png' }
  ];
  activeSlideIndex = signal<number>(1); // Mặc định Emerald (Index 1) ở giữa
  private autoplayIntervalId: any;

  ngOnInit() {
    this.startAutoplay();
  }

  ngOnDestroy() {
    this.stopAutoplay();
  }

  selectStyle(style: 'gold' | 'emerald' | 'rustic') {
    this.selectedStyle.set(style);
  }

  // Điều khiển Slider ở Hero
  nextSlide() {
    this.activeSlideIndex.update(idx => (idx === this.heroMenus.length - 1 ? 0 : idx + 1));
  }

  prevSlide() {
    this.activeSlideIndex.update(idx => (idx === 0 ? this.heroMenus.length - 1 : idx - 1));
  }

  selectSlide(index: number) {
    this.activeSlideIndex.set(index);
    this.resetAutoplay();
  }

  private startAutoplay() {
    if (isPlatformBrowser(this.platformId)) {
      this.autoplayIntervalId = setInterval(() => {
        this.nextSlide();
      }, 3500);
    }
  }

  private stopAutoplay() {
    if (this.autoplayIntervalId) {
      clearInterval(this.autoplayIntervalId);
    }
  }

  private resetAutoplay() {
    this.stopAutoplay();
    this.startAutoplay();
  }

  onStartDesigning() {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dichvu']);
    } else {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/dichvu' } });
    }
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }

  logout() {
    this.authService.logout();
  }
}
