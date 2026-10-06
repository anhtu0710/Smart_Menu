import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/auth`;

  // Reactive state với Angular Signals
  private userSignal = signal<User | null>(this.getStoredUser());
  private tokenSignal = signal<string | null>(this.getStoredToken());

  // Expose signal dưới dạng read-only
  readonly currentUser = this.userSignal.asReadonly();
  readonly token = this.tokenSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.userSignal());

  constructor() {
    // Tự động kiểm tra session & lấy thông tin user hiện tại từ backend
    if (this.userSignal()) {
      this.fetchCurrentUser().subscribe();
    }
  }

  /**
   * Đăng nhập với Java Spring Boot REST API
   * Endpoint: POST /api/v1/auth/login
   */
  login(request: LoginRequest): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/login`, request, { withCredentials: true }).pipe(
      tap((response) => {
        if (response && response.success && response.data) {
          const user: User = response.data.user || response.data;
          this.setSessionUser(user, request.rememberMe ?? false);
        }
      })
    );
  }

  /**
   * Đăng ký tài khoản mới với Java Spring Boot REST API
   * Endpoint: POST /api/v1/auth/register
   */
  register(request: RegisterRequest): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.apiUrl}/register`, request, { withCredentials: true });
  }

  /**
   * Lấy thông tin người dùng hiện tại từ Java Backend
   * Endpoint: GET /api/v1/auth/me
   */
  fetchCurrentUser(): Observable<User | null> {
    return this.http.get<ApiResponse<User>>(`${this.apiUrl}/me`, { withCredentials: true }).pipe(
      map(res => res.data),
      tap(user => {
        if (user) {
          this.userSignal.set(user);
          this.updateStoredUser(user);
        }
      }),
      catchError(() => {
        this.logout();
        return of(null);
      })
    );
  }

  /**
   * Đăng xuất tài khoản & xóa session
   */
  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).subscribe({
      error: () => {}
    });

    localStorage.removeItem('access_token');
    sessionStorage.removeItem('access_token');
    localStorage.removeItem('current_user');
    sessionStorage.removeItem('current_user');

    this.userSignal.set(null);
    this.tokenSignal.set(null);
  }

  private setSessionUser(user: User, rememberMe: boolean): void {
    const storage = rememberMe ? localStorage : sessionStorage;
    const token = 'session_' + (user.id || Date.now());
    storage.setItem('current_user', JSON.stringify(user));
    storage.setItem('access_token', token);
    this.userSignal.set(user);
    this.tokenSignal.set(token);
  }

  private getStoredToken(): string | null {
    return localStorage.getItem('access_token') || sessionStorage.getItem('access_token');
  }

  private getStoredUser(): User | null {
    const userStr = localStorage.getItem('current_user') || sessionStorage.getItem('current_user');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  }

  private updateStoredUser(user: User): void {
    const storage = localStorage.getItem('current_user') ? localStorage : sessionStorage;
    storage.setItem('current_user', JSON.stringify(user));
  }
}
