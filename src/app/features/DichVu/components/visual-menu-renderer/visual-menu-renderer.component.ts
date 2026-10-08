import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { VisualMenuBlueprint } from '../../../../core/models/ai-menu.model';
import { AiMenuService } from '../../../../core/services/ai-menu.service';

@Component({
  selector: 'app-visual-menu-renderer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './visual-menu-renderer.component.html',
  styleUrls: ['./visual-menu-renderer.component.scss']
})
export class VisualMenuRendererComponent implements OnChanges, OnDestroy {
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
  private fetchTimer: ReturnType<typeof setTimeout> | null = null;
  private artworkRequest?: Subscription;
  private activeRequestKey: string | null = null;
  private completedRequestKey: string | null = null;

  constructor(private aiMenuService: AiMenuService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['styleId'] || changes['currentSessionId'] || changes['blueprint']) {
      this.scheduleFinalMenuImageFetch();
    }
  }

  ngOnDestroy(): void {
    if (this.fetchTimer) {
      clearTimeout(this.fetchTimer);
    }
    this.artworkRequest?.unsubscribe();
  }

  debugMode = false;

  toggleDebugMode(): void {
    this.debugMode = !this.debugMode;
    console.log(`[FINAL-MENU] Toggle Debug Mode: ${this.debugMode}`);
    this.scheduleFinalMenuImageFetch();
  }

  /**
   * Inputs are populated in a few consecutive change-detection passes after
   * analysis. Delay briefly so only the final session/style pair triggers an
   * expensive backend artwork render.
   */
  private scheduleFinalMenuImageFetch(): void {
    if (!this.currentSessionId) {
      this.artworkRequest?.unsubscribe();
      this.activeRequestKey = null;
      this.completedRequestKey = null;
      this.finalImageUrl = null;
      return;
    }

    if (this.fetchTimer) {
      clearTimeout(this.fetchTimer);
    }

    this.fetchTimer = setTimeout(() => {
      this.fetchTimer = null;
      this.fetchFinalMenuImage();
    }, 75);
  }

  /**
   * Gọi Backend API sinh ảnh Final Menu do AI Multimodal tạo ra dựa trên ảnh mẫu Backend
   */
  fetchFinalMenuImage(): void {
    if (!this.currentSessionId) return;

    const requestKey = `${this.currentSessionId}:${this.styleId}:${this.debugMode}`;
    if (this.activeRequestKey === requestKey || (this.completedRequestKey === requestKey && this.finalImageUrl)) {
      return;
    }

    this.artworkRequest?.unsubscribe();
    this.activeRequestKey = requestKey;

    console.log(`[AI-MENU-CLIENT] Fetching AI Artwork | styleId=${this.styleId} | sessionId=${this.currentSessionId}`);

    this.isGeneratingImage = true;
    this.isImageLoaded = false;
    this.isImageError = false;

    this.artworkRequest = this.aiMenuService.generateFinalMenuImage(this.currentSessionId, this.styleId, this.debugMode).subscribe({
      next: (res) => {
        if (this.activeRequestKey !== requestKey) return;

        this.activeRequestKey = null;
        this.isGeneratingImage = false;
        this.artworkRequest = undefined;
        if (res && res.success && res.data && res.data.finalImageUrl) {
          let url = res.data.finalImageUrl;
          if (url.startsWith('data:image/')) {
            this.finalImageUrl = url.replace(/[\r\n\s]/g, '');
          } else {
            // In production the frontend proxies /api to the backend. Keeping
            // this relative also works through Cloudflare HTTPS; hard-coding
            // localhost would otherwise point to the visitor's own machine.
            this.finalImageUrl = `${url}${url.includes('?') ? '&' : '?'}v=${Date.now()}`;
          }

          this.canvasWidth = res.data.canvasWidth || 800;
          this.canvasHeight = res.data.canvasHeight || 1131;
          this.aspectRatio = res.data.aspectRatio || (this.canvasWidth / this.canvasHeight);
          this.manifest = res.data.manifest || [];
          this.completedRequestKey = requestKey;

          console.log(`[AI-MENU-CLIENT SUCCESS] Received AI Artwork for Session ${this.currentSessionId} (${this.canvasWidth}x${this.canvasHeight})`);
        } else {
          this.isImageError = true;
          this.finalImageUrl = null;
          this.completedRequestKey = null;
        }
      },
      error: (err) => {
        if (this.activeRequestKey !== requestKey) return;

        this.activeRequestKey = null;
        this.isGeneratingImage = false;
        this.artworkRequest = undefined;
        this.isImageError = true;
        this.finalImageUrl = null;
        this.completedRequestKey = null;
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
