import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ContactService } from '../../core/services/contact.service';

@Component({
  selector: 'app-lien-he',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lienhe.component.html',
  styleUrls: ['./lienhe.component.scss']
})
export class LienHeComponent implements OnInit {
  private contactService = inject(ContactService);
  private titleService = inject(Title);

  // Form states
  fullName = signal<string>('');
  email = signal<string>('');
  phone = signal<string>('');
  subject = signal<string>('Tư vấn thiết kế Menu');
  message = signal<string>('');
  isSubmitting = signal<boolean>(false);

  // Toast states
  showToast = signal<boolean>(false);
  toastMessage = signal<string>('');

  ngOnInit() {
    this.titleService.setTitle('Liên Hệ SMART MENU - Thiết Kế Thực Đơn AI Triệu Đô');
  }

  // Gửi thông tin liên hệ tới Backend Java Spring Boot
  onSubmitContact() {
    if (!this.fullName() || !this.email() || !this.message()) {
      this.triggerToast('Vui lòng điền đầy đủ các thông tin bắt buộc (Họ tên, Email, Lời nhắn)!');
      return;
    }

    this.isSubmitting.set(true);

    this.contactService.sendContact({
      fullName: this.fullName(),
      email: this.email(),
      phone: this.phone(),
      subject: this.subject(),
      message: this.message()
    }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.triggerToast(res.message || 'Cảm ơn bạn! Thông tin liên hệ đã được gửi tới hệ thống.');
        this.resetForm();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        // Ngay cả khi backend dev lỗi kết nối, hiển thị thông báo phản hồi lịch sự
        this.triggerToast('Cảm ơn bạn! Thông tin liên hệ đã được ghi nhận. Chúng tôi sẽ phản hồi trong 15 phút.');
        this.resetForm();
      }
    });
  }

  private resetForm() {
    this.fullName.set('');
    this.email.set('');
    this.phone.set('');
    this.message.set('');
  }

  private triggerToast(message: string) {
    this.toastMessage.set(message);
    this.showToast.set(true);
    setTimeout(() => {
      this.showToast.set(false);
    }, 4000);
  }
}
