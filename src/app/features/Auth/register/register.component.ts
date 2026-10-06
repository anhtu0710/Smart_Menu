import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  authService = inject(AuthService);
  router = inject(Router);
  titleService = inject(Title);

  // Form states
  fullName = signal<string>('');
  email = signal<string>('');
  phone = signal<string>('');
  password = signal<string>('');
  confirmPassword = signal<string>('');
  agreeTerms = signal<boolean>(false);

  showPassword = signal<boolean>(false);
  showConfirmPassword = signal<boolean>(false);
  isLoading = signal<boolean>(false);

  // Notification states
  showToast = signal<boolean>(false);
  toastMessage = signal<string>('');
  toastType = signal<'success' | 'error'>('success');

  ngOnInit() {
    this.titleService.setTitle('Đăng Ký Tài Khoản SMART MENU - Tạo Menu AI Triệu Đô');
  }

  togglePasswordVisibility() {
    this.showPassword.update(v => !v);
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword.update(v => !v);
  }

  onRegister() {
    const cleanEmail = this.email().trim();
    const cleanName = this.fullName().trim();
    const cleanPhone = this.phone().trim();
    const cleanPass = this.password().trim();
    const cleanConfirm = this.confirmPassword().trim();

    if (!cleanName || !cleanEmail || !cleanPass || !cleanConfirm) {
      this.triggerToast('Vui lòng điền đầy đủ tất cả các thông tin bắt buộc!', 'error');
      return;
    }

    if (!cleanEmail.includes('@') || cleanEmail.includes(' ')) {
      this.triggerToast('Định dạng địa chỉ Email không hợp lệ!', 'error');
      return;
    }

    if (cleanPass.length < 6) {
      this.triggerToast('Mật khẩu phải chứa ít nhất 6 ký tự!', 'error');
      return;
    }

    if (cleanPass !== cleanConfirm) {
      this.triggerToast('Mật khẩu xác nhận không trùng khớp!', 'error');
      return;
    }

    if (!this.agreeTerms()) {
      this.triggerToast('Bạn phải đồng ý với Điều khoản dịch vụ và Chính sách bảo mật!', 'error');
      return;
    }

    this.isLoading.set(true);

    // Gọi Java Backend Spring Boot API Đăng ký với dữ liệu đã được làm sạch
    this.authService.register({
      fullName: cleanName,
      email: cleanEmail,
      phone: cleanPhone,
      password: cleanPass
    }).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (!res || !res.success) {
          const errorMsg = res?.message || 'Đăng ký thất bại. Email này có thể đã được đăng ký!';
          this.triggerToast(errorMsg, 'error');
          return;
        }
        this.triggerToast(res.message || 'Đăng ký tài khoản thành công! Đang chuyển đến trang đăng nhập...', 'success');
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1500);
      },
      error: (err) => {
        this.isLoading.set(false);
        const errorMsg = err?.error?.message || err?.message || 'Đăng ký thất bại. Email này có thể đã được đăng ký!';
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
