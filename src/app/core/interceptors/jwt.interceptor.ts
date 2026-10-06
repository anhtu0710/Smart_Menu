import { HttpInterceptorFn } from '@angular/common/http';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  // Lấy token từ localStorage hoặc sessionStorage
  const token = localStorage.getItem('access_token') || sessionStorage.getItem('access_token');

  // Đặt header Authorization nếu có token và không thuộc endpoint public login/register
  if (token) {
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned);
  }

  return next(req);
};
