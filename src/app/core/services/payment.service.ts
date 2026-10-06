import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AccessCheckResponse, PaymentCreateResponse, PaymentStatusResponse } from '../models/payment.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = environment.apiUrl; // e.g., http://localhost:8080/api/v1

  private getEffectiveUserId(): string | null {
    const user = this.authService.currentUser();
    return user?.id ? String(user.id) : null;
  }

  /**
   * Kiểm tra quyền sử dụng dịch vụ tạo menu của User:
   * Trả về: FREE, PAID (đã thanh toán chưa dùng), hoặc PAYMENT_REQUIRED (bắt buộc thanh toán)
   */
  checkAccess(customUserId?: string | number): Observable<ApiResponse<AccessCheckResponse>> {
    let params = new HttpParams();
    const userId = customUserId ? String(customUserId) : this.getEffectiveUserId();
    if (userId) {
      params = params.set('userId', userId);
    }

    return this.http.get<ApiResponse<AccessCheckResponse>>(`${this.apiUrl}/service/check-access`, {
      params,
      withCredentials: true
    });
  }

  /**
   * Tạo giao dịch thanh toán mới cho lần thiết kế menu (50.000 VNĐ)
   */
  createPayment(customUserId?: string | number): Observable<ApiResponse<PaymentCreateResponse>> {
    let params = new HttpParams();
    const userId = customUserId ? String(customUserId) : this.getEffectiveUserId();
    if (userId) {
      params = params.set('userId', userId);
    }

    return this.http.post<ApiResponse<PaymentCreateResponse>>(`${this.apiUrl}/payment/create`, {}, {
      params,
      withCredentials: true
    });
  }

  /**
   * Kiểm tra trạng thái thanh toán (polling)
   */
  checkPaymentStatus(paymentId: number): Observable<ApiResponse<PaymentStatusResponse>> {
    return this.http.get<ApiResponse<PaymentStatusResponse>>(`${this.apiUrl}/payment/check-status/${paymentId}`, {
      withCredentials: true
    });
  }

  /**
   * Giả lập thanh toán thành công (dành cho Demo & Test hoặc nút xác nhận nhanh)
   */
  mockConfirmPayment(paymentId: number): Observable<ApiResponse<PaymentStatusResponse>> {
    return this.http.post<ApiResponse<PaymentStatusResponse>>(`${this.apiUrl}/payment/mock-confirm/${paymentId}`, {}, {
      withCredentials: true
    });
  }
}
