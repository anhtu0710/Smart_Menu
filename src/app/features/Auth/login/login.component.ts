import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  authService = inject(AuthService);
  router = inject(Router);
  route = inject(ActivatedRoute);
  titleService = inject(Title);

  // Form states
  email = signal<string>('');
  password = signal<string>('');
  rememberMe = signal<boolean>(false);
  showPassword = signal<boolean>(false);
  isLoading = signal<boolean>(false);

  // Notification states
  showToast = signal<boolean>(false);
  toastMessage = signal<string>('');
  toastType = signal<'success' | 'error'>('success');

  private returnUrl: string = '/dichvu';

  ngOnInit() {
    this.titleService.setTitle('Đăng Nhập SMART MENU - Giải Pháp Thiết Kế Menu AI');
    
    // Lấy queryParam returnUrl nếu được chuyển hướng từ AuthGuard
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dichvu';
    if (this.route.snapshot.queryParams['expired'] === 'true') {
      this.triggerToast('Phiên làm việc của bạn đã hết hạn, vui lòng đăng nhập lại.', 'error');
    }

    // Nếu đã đăng nhập rồi -> chuyển hướng luôn
    if (this.authService.isAuthenticated()) {
      this.router.navigateByUrl(this.returnUrl);
    }
  }

  togglePasswordVisibility() {
    this.showPassword.update(v => !v);
  }

  onLogin() {
    if (!this.email() || !this.password()) {
      this.triggerToast('Vui lòng điền đầy đủ Email và Mật khẩu!', 'error');
      return;
    }

    if (!this.email().includes('@')) {
      this.triggerToast('Định dạng địa chỉ Email không hợp lệ!', 'error');
      return;
    }

    this.isLoading.set(true);

    // Gọi API Đăng nhập tới Backend Java Spring Boot
    this.authService.login({
      email: this.email(),
      password: this.password(),
      rememberMe: this.rememberMe()
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (!res || !res.success) {
          const errorMsg = res?.message || 'Email hoặc mật khẩu không chính xác!';
          this.triggerToast(errorMsg, 'error');
          return;
        }
        this.triggerToast(res.message || 'Đăng nhập thành công! Đang chuyển hướng...', 'success');
        setTimeout(() => {
          this.router.navigateByUrl(this.returnUrl);
        }, 1000);
      },
      error: (err) => {
        this.isLoading.set(false);
        const errorMsg = err?.error?.message || err?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra tài khoản và mật khẩu!';
        this.triggerToast(errorMsg, 'error');
      }
    });
  }

  triggerToast(message: string, type: 'success' | 'error') {
    this.toastMessage.set(message);
    this.toastType.set(type);
    this.showToast.set(true);
    setTimeout(() => {
      this.showToast.set(false);
    }, 4000);
  }
}
