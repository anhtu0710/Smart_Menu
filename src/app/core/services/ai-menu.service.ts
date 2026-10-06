import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  UploadResponseDto,
  FullAnalysisResponseData,
  GeneratedMenu,
  AIAnalysisResult,
  FnbRuleDto
} from '../models/ai-menu.model';

import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AiMenuService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private uploadUrl = `${environment.apiUrl}/upload`;
  private smartCoffeeUrl = `${environment.apiUrl}/smartcoffee`;

  /**
   * Upload File Excel kinh doanh & File Ảnh menu cũ lên Spring Boot Backend
   * Endpoint: POST /api/v1/upload/menu-analysis
   */
  uploadMenuAnalysis(excelFile: File, menuImage: File): Observable<ApiResponse<UploadResponseDto>> {
    const formData = new FormData();
    formData.append('excelFile', excelFile);
    formData.append('menuImage', menuImage);
    const user = this.authService.currentUser();
    if (user?.id) {
      formData.append('userId', String(user.id));
    }
    return this.http.post<ApiResponse<UploadResponseDto>>(`${this.uploadUrl}/menu-analysis`, formData, {
      withCredentials: true
    });
  }

  /**
   * Bắt đầu phân tích toàn bộ Workflow dựa theo sessionId đã tạo ở bước Upload
   * Endpoint: POST /api/v1/smartcoffee/analyze-full/{sessionId}
   */
  analyzeFullWorkflow(sessionId: number | string): Observable<ApiResponse<FullAnalysisResponseData>> {
    return this.http.post<ApiResponse<FullAnalysisResponseData>>(`${this.smartCoffeeUrl}/analyze-full/${sessionId}`, {}, {
      withCredentials: true
    });
  }

  /**
   * Lấy lại kết quả phân tích theo sessionId
   * Endpoint: GET /api/v1/smartcoffee/analysis/{sessionId}
   */
  getAnalysis(sessionId: number | string): Observable<ApiResponse<FullAnalysisResponseData>> {
    return this.http.get<ApiResponse<FullAnalysisResponseData>>(`${this.smartCoffeeUrl}/analysis/${sessionId}`, {
      withCredentials: true
    });
  }

  /**
   * Lấy menu mới được đề xuất theo sessionId
   * Endpoint: GET /api/v1/smartcoffee/menu/{sessionId}/generated
   */
  getGeneratedMenu(sessionId: number | string): Observable<ApiResponse<GeneratedMenu>> {
    return this.http.get<ApiResponse<GeneratedMenu>>(`${this.smartCoffeeUrl}/menu/${sessionId}/generated`);
  }

  /**
   * Lấy chiến lược AI đã phân tích theo sessionId
   * Endpoint: GET /api/v1/smartcoffee/menu/{sessionId}/strategy
   */
  getAIStrategy(sessionId: number | string): Observable<ApiResponse<AIAnalysisResult>> {
    return this.http.get<ApiResponse<AIAnalysisResult>>(`${this.smartCoffeeUrl}/menu/${sessionId}/strategy`);
  }

  /**
   * Lấy dữ liệu VisualMenuBlueprint & SelectedStyle theo sessionId
   * Endpoint: GET /api/v1/smartcoffee/menu/{sessionId}/blueprint
   */
  getMenuBlueprint(sessionId: number | string): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.smartCoffeeUrl}/menu/${sessionId}/blueprint`);
  }

  /**
   * Đổi Style cho Menu (Không gọi lại AI, không thay đổi Blueprint)
   * Endpoint: POST /api/v1/smartcoffee/menu/{sessionId}/style
   */
  updateMenuStyle(sessionId: number | string, styleId: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.smartCoffeeUrl}/menu/${sessionId}/style`, { styleId });
  }

  /**
   * Gọi API sinh ảnh Final Menu theo kiến trúc AI Layout + Locked Text Composite Renderer
   * Endpoint: POST /api/v1/smartcoffee/menu/{sessionId}/generate-final-menu
   */
  generateFinalMenuImage(sessionId: number | string, styleId: string, debug: boolean = false): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.smartCoffeeUrl}/menu/${sessionId}/generate-final-menu`, { styleId, debug });
  }

}

