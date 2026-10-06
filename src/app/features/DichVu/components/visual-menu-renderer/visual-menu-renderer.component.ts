import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VisualMenuBlueprint } from '../../../../core/models/ai-menu.model';
import { AiMenuService } from '../../../../core/services/ai-menu.service';

@Component({
  selector: 'app-visual-menu-renderer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './visual-menu-renderer.component.html',
  styleUrls: ['./visual-menu-renderer.component.scss']
})
export class VisualMenuRendererComponent implements OnChanges {
  @Input() blueprint: VisualMenuBlueprint | null = null;
  @Input() styleId: string = 'HIEN_DAI';
  @Input() currentSessionId: number | string | null = null;

  // AI Artwork Renderer State (Pure AI Output - Zero Frontend Template)
  finalImageUrl: string | null = null;
  isGeneratingImage = false;
  isImageLoaded = false;
  isImageError = false;
  canvasWidth = 800;
  canvasHeight = 1131;
  aspectRatio = 800 / 1131;
  manifest: any[] = [];
  isFullscreenModalOpen = false;

  constructor(private aiMenuService: AiMenuService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['styleId'] || changes['currentSessionId'] || changes['blueprint']) {
      this.fetchFinalMenuImage();
    }
  }

  debugMode = false;

  toggleDebugMode(): void {
    this.debugMode = !this.debugMode;
    console.log(`[FINAL-MENU] Toggle Debug Mode: ${this.debugMode}`);
    this.fetchFinalMenuImage();
  }

  /**
   * Gọi Backend API sinh ảnh Final Menu do AI Multimodal tạo ra dựa trên ảnh mẫu Backend
   */
  fetchFinalMenuImage(): void {
    if (!this.currentSessionId) return;

    console.log(`[AI-MENU-CLIENT] Fetching AI Artwork | styleId=${this.styleId} | sessionId=${this.currentSessionId}`);

    this.isGeneratingImage = true;
    this.isImageLoaded = false;
    this.isImageError = false;

    this.aiMenuService.generateFinalMenuImage(this.currentSessionId, this.styleId, this.debugMode).subscribe({
      next: (res) => {
        this.isGeneratingImage = false;
        if (res && res.success && res.data && res.data.finalImageUrl) {
          let url = res.data.finalImageUrl;
          if (url.startsWith('data:image/')) {
            this.finalImageUrl = url.replace(/[\r\n\s]/g, '');
          } else {
            const host = url.startsWith('http') ? '' : 'http://localhost:8080';
            this.finalImageUrl = `${host}${url}${url.includes('?') ? '&' : '?'}v=${Date.now()}`;
          }

          this.canvasWidth = res.data.canvasWidth || 800;
          this.canvasHeight = res.data.canvasHeight || 1131;
          this.aspectRatio = res.data.aspectRatio || (this.canvasWidth / this.canvasHeight);
          this.manifest = res.data.manifest || [];

          console.log(`[AI-MENU-CLIENT SUCCESS] Received AI Artwork for Session ${this.currentSessionId} (${this.canvasWidth}x${this.canvasHeight})`);
        } else {
          this.isImageError = true;
          this.finalImageUrl = null;
        }
      },
      error: (err) => {
        this.isGeneratingImage = false;
        this.isImageError = true;
        this.finalImageUrl = null;
        console.warn('[AI-MENU-CLIENT WARN] Backend AI generation error:', err);
      }
    });
  }

  onFinalImageLoaded(): void {
    this.isImageLoaded = true;
    this.isImageError = false;
    console.log('[FINAL-MENU-UI] AI Artwork displayed successfully | styleId=' + this.styleId);
  }

  onFinalImageError(event: Event): void {
    this.isImageLoaded = false;
    this.isImageError = true;
    console.error('[FINAL-MENU-UI] IMAGE RENDER FAILED', event);
  }

  toggleFullscreenModal(): void {
    this.isFullscreenModalOpen = !this.isFullscreenModalOpen;
  }

  downloadFinalImage(): void {
    if (!this.finalImageUrl) return;
    const a = document.createElement('a');
    a.href = this.finalImageUrl;
    a.download = `SmartMenu_AI_Artwork_${this.styleId}_Session${this.currentSessionId}.png`;
    a.click();
  }
}
