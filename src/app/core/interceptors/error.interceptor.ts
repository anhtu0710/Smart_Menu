import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = '';

      if (error.error instanceof ErrorEvent) {
        // Lỗi phía Client hoặc Network
        errorMessage = `Lỗi kết nối: ${error.error.message}`;
      } else {
        // Ưu tiên đọc thông báo lỗi từ Java Backend Response
        if (error.error && typeof error.error === 'object') {
          if (error.error.message) {
            errorMessage = error.error.message;
          } else if (error.error.errors) {
            if (Array.isArray(error.error.errors)) {
              errorMessage = error.error.errors.join(', ');
            } else if (typeof error.error.errors === 'object') {
              errorMessage = Object.values(error.error.errors).join(', ');
            }
          }
        }

        // Nếu chưa có message cụ thể từ server mới dùng fallback status code
        if (!errorMessage) {
          switch (error.status) {
            case 401:
              errorMessage = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!';
              break;
            case 403:
              errorMessage = 'Bạn không có quyền truy cập vào chức năng này!';
              break;
            case 404:
              errorMessage = 'Không tìm thấy tài nguyên trên máy chủ!';
              break;
            case 500:
              errorMessage = 'Lỗi máy chủ Backend Java Spring Boot!';
              break;
            default:
              errorMessage = 'Đã xảy ra lỗi kết nối hệ thống. Vui lòng thử lại!';
          }
        }

        if (error.status === 401) {
          localStorage.removeItem('access_token');
          sessionStorage.removeItem('access_token');
          localStorage.removeItem('current_user');
          router.navigate(['/login'], { queryParams: { returnUrl: router.url, expired: 'true' } });
        }
      }

      console.error(`[HttpError ${error.status}]:`, errorMessage, error);
      return throwError(() => new Error(errorMessage));
    })
  );
};
