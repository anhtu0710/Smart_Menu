export interface User {
  id?: number | string;
  name: string;
  email: string;
  phone?: string;
  role?: 'ROLE_USER' | 'ROLE_ADMIN' | 'ROLE_MANAGER';
  avatarUrl?: string;
  freeUsed?: boolean;
  createdAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  user: User;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}
