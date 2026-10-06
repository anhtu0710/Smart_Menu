import { Injectable } from '@angular/core';
import { CoffeeTemplateDefinition, CoffeeTemplateId, TemplatePageDto, TemplateLayoutConfig, ContentBlockConfig, SinglePageMenuView, BlockDishGroup } from '../models/coffee-template.model';
import { VisualMenuBlueprint, BlueprintSectionDto, ProductItemDto } from '../models/ai-menu.model';

@Injectable({
  providedIn: 'root'
})
export class CoffeeTemplateService {

  private readonly templates: Record<CoffeeTemplateId, CoffeeTemplateDefinition> = {
    COFFEE_MODERN_01: {
      templateId: 'COFFEE_MODERN_01',
      templateName: 'Mẫu Coffee Hiện Đại',
      badgeText: 'HIỆN ĐẠI',
      description: 'Phong cách Dark Navy hiện đại, khung xanh sẫm bên phải bối cảnh smoothie nhiệt đới.',
      backgroundAsset: 'assets/templates/coffee_modern_bg.png',
      thumbnailAsset: 'assets/templates/coffee_modern_bg.png',
      primaryColor: '#f59e0b',
      accentColor: '#fbbf24',
      textColor: '#ffffff',
      cardBgColor: 'rgba(15, 23, 42, 0.75)',
      maxProductsPerPage: 12,
      hasHeroSection: true,
      hasComboSection: true
    },
    COFFEE_LUXURY_01: {
      templateId: 'COFFEE_LUXURY_01',
      templateName: 'Mẫu Coffee Sang Trọng',
      badgeText: 'SANG TRỌNG',
      description: 'Phong cách pastel ngọt ngào, khung thẻ trắng trung tâm họa tiết trà bánh nghệ thuật.',
      backgroundAsset: 'assets/templates/coffee_luxury_bg.png',
      thumbnailAsset: 'assets/templates/coffee_luxury_bg.png',
      primaryColor: '#be185d',
      accentColor: '#db2777',
      textColor: '#1f2937',
      cardBgColor: 'rgba(255, 255, 255, 0.95)',
      maxProductsPerPage: 14,
      hasHeroSection: true,
      hasComboSection: true
    },
    COFFEE_TRADITIONAL_01: {
      templateId: 'COFFEE_TRADITIONAL_01',
      templateName: 'Mẫu Coffee Truyền Thống',
      badgeText: 'TRUYỀN THỐNG',
      description: 'Phong cách giấy da vintage mộc mạc, họa tiết hạt mộc & tách cà phê espresso đậm đà.',
      backgroundAsset: 'assets/templates/coffee_traditional_bg.png',
      thumbnailAsset: 'assets/templates/coffee_traditional_bg.png',
      primaryColor: '#451a03',
      accentColor: '#78350f',
      textColor: '#292524',
      cardBgColor: 'rgba(255, 251, 235, 0.85)',
      maxProductsPerPage: 12,
      hasHeroSection: true,
      hasComboSection: true
    },
    COFFEE_YOUTHFUL_01: {
      templateId: 'COFFEE_YOUTHFUL_01',
      templateName: 'Mẫu Coffee Trẻ Trung',
      badgeText: 'TRẺ TRUNG',
      description: 'Phong cách năng động khoáng đạt, nền sáng bố cục chéo hòa quyện 3 ly trà & boba.',
      backgroundAsset: 'assets/templates/coffee_youthful_bg.png',
      thumbnailAsset: 'assets/templates/coffee_youthful_bg.png',
      primaryColor: '#065f46',
      accentColor: '#059669',
      textColor: '#0f172a',
      cardBgColor: 'rgba(255, 255, 255, 0.88)',
      maxProductsPerPage: 12,
      hasHeroSection: true,
      hasComboSection: true
    }
  };

  getAllTemplates(): CoffeeTemplateDefinition[] {
    return Object.values(this.templates);
  }

  getTemplateById(templateId: string): CoffeeTemplateDefinition {
    const key = (templateId || 'COFFEE_MODERN_01').toUpperCase() as CoffeeTemplateId;
    return this.templates[key] || this.templates.COFFEE_MODERN_01;
  }

  /**
   * Thuật toán Phân trang Động (Dynamic Pagination):
   * Tự động tính toán chia Blueprint thành danh sách các trang `TemplatePageDto` mà KHÔNG LÀM MẤT MÓN NÀO.
   */
  paginateBlueprint(blueprint: VisualMenuBlueprint | null, templateId: string): TemplatePageDto[] {
    const template = this.getTemplateById(templateId);
    if (!blueprint || !blueprint.sections || blueprint.sections.length === 0) {
      return [{
        pageIndex: 0,
        totalPages: 1,
        template,
        sections: [],
        heroDishes: [],
        structuredCombos: []
      }];
    }

    const maxItemsPerPage = template.maxProductsPerPage || 16;
    const pages: TemplatePageDto[] = [];
    
    let currentPageSections: BlueprintSectionDto[] = [];
    let currentItemCount = 0;

    for (const sec of blueprint.sections) {
      if (!sec.dishes || sec.dishes.length === 0) continue;

      let remainingDishes = [...sec.dishes];
      while (remainingDishes.length > 0) {
        const availableSlots = maxItemsPerPage - currentItemCount;
        
        if (availableSlots <= 0) {
          // Push trang hiện tại và tạo trang mới
          pages.push({
            pageIndex: pages.length,
            totalPages: 0,
            template,
            sections: currentPageSections,
            heroDishes: pages.length === 0 ? (blueprint.heroDishes || []) : [],
            structuredCombos: []
          });
          currentPageSections = [];
          currentItemCount = 0;
          continue;
        }

        const chunk = remainingDishes.slice(0, availableSlots);
        remainingDishes = remainingDishes.slice(availableSlots);

        currentPageSections.push({
          categoryName: sec.categoryName,
          priority: sec.priority,
          dishes: chunk
        });
        currentItemCount += chunk.length;
      }
    }

    if (currentPageSections.length > 0 || pages.length === 0) {
      pages.push({
        pageIndex: pages.length,
        totalPages: 0,
        template,
        sections: currentPageSections,
        heroDishes: pages.length === 0 ? (blueprint.heroDishes || []) : [],
        structuredCombos: (blueprint.structuredCombos || [])
      });
    }

    const total = pages.length;
    pages.forEach(p => p.totalPages = total);
    return pages;
  }

  /**
   * Đọc giá hiển thị thực tế của sản phẩm từ Blueprint:
   * Ưu tiên recommendedPrice từ priceRecommendations nếu hợp lệ, ngược lại dùng originalPrice.
   * Renderer KHÔNG tự tính lại giá.
   */
  getEffectivePrice(dish: ProductItemDto, blueprint: VisualMenuBlueprint | null): number {
    if (!dish) return 0;
    if (blueprint && blueprint.priceRecommendations && dish.name) {
      const rec = blueprint.priceRecommendations.find(
        p => p.productName && p.productName.trim().toLowerCase() === dish.name.trim().toLowerCase()
      );
      if (rec && rec.recommendedPrice && rec.recommendedPrice > 0) {
        return rec.recommendedPrice;
      }
    }
    return dish.originalPrice || 0;
  }

  /**
   * Kiểm tra giao cắt Bounding Box Intersection giữa Content Block và Exclusion Zone
   */
  validateContentBlocksAgainstExclusionZones(layoutConfig: TemplateLayoutConfig): boolean {
    if (!layoutConfig.exclusionZones || layoutConfig.exclusionZones.length === 0) {
      return true;
    }

    let isValid = true;
    for (const block of layoutConfig.blocks) {
      const blockRight = block.leftPercent + block.widthPercent;
      const blockBottom = block.topPercent + block.heightPercent;

      for (const zone of layoutConfig.exclusionZones) {
        const zoneRight = zone.leftPercent + zone.widthPercent;
        const zoneBottom = zone.topPercent + zone.heightPercent;

        const intersects = !(
          block.leftPercent >= zoneRight ||
          blockRight <= zone.leftPercent ||
          block.topPercent >= zoneBottom ||
          blockBottom <= zone.topPercent
        );

        if (intersects) {
          console.warn(`[LAYOUT WARN] Content block '${block.id}' (left:${block.leftPercent}%, top:${block.topPercent}%) intersects exclusion zone '${zone.id}' in template '${layoutConfig.templateId}'`);
          isValid = false;
        }
      }
    }
    return isValid;
  }

  /**
   * BẢNG CẤU HÌNH BẢN ĐỒ LAYOUT CỐ ĐỊNH THEO KÍCH THƯỚC VÀ VÙNG TRỐNG THỰC TẾ CỦA 4 ẢNH BACKGROUND
   */
  private readonly layoutConfigs: Record<CoffeeTemplateId, TemplateLayoutConfig> = {
    COFFEE_MODERN_01: {
      templateId: 'COFFEE_MODERN_01',
      canvasWidth: 435,
      canvasHeight: 627,
      aspectRatio: 0.6938,
      blocks: [
        { id: 'modern-top-right', leftPercent: 46, topPercent: 8, widthPercent: 48, heightPercent: 26, capacityWeight: 10 },
        { id: 'modern-mid-right', leftPercent: 46, topPercent: 37, widthPercent: 48, heightPercent: 26, capacityWeight: 10 },
        { id: 'modern-lower-right', leftPercent: 46, topPercent: 66, widthPercent: 48, heightPercent: 26, capacityWeight: 10 }
      ],
      exclusionZones: [
        { id: 'smoothie-left-panel', leftPercent: 0, topPercent: 0, widthPercent: 43, heightPercent: 100 }
      ]
    },
    COFFEE_LUXURY_01: {
      templateId: 'COFFEE_LUXURY_01',
      canvasWidth: 438,
      canvasHeight: 632,
      aspectRatio: 0.6930,
      blocks: [
        { id: 'luxury-top', leftPercent: 21, topPercent: 16, widthPercent: 58, heightPercent: 21, capacityWeight: 10 },
        { id: 'luxury-mid', leftPercent: 21, topPercent: 39, widthPercent: 58, heightPercent: 21, capacityWeight: 10 },
        { id: 'luxury-bottom', leftPercent: 21, topPercent: 62, widthPercent: 58, heightPercent: 21, capacityWeight: 10 }
      ],
      exclusionZones: [
        { id: 'pastel-border-top', leftPercent: 0, topPercent: 0, widthPercent: 100, heightPercent: 13 },
        { id: 'pastel-border-bottom', leftPercent: 0, topPercent: 85, widthPercent: 100, heightPercent: 15 },
        { id: 'pastel-border-left', leftPercent: 0, topPercent: 0, widthPercent: 19, heightPercent: 100 },
        { id: 'pastel-border-right', leftPercent: 81, topPercent: 0, widthPercent: 19, heightPercent: 100 }
      ]
    },
    COFFEE_TRADITIONAL_01: {
      templateId: 'COFFEE_TRADITIONAL_01',
      canvasWidth: 440,
      canvasHeight: 606,
      aspectRatio: 0.7261,
      blocks: [
        { id: 'traditional-upper-left', leftPercent: 6, topPercent: 8, widthPercent: 54, heightPercent: 42, capacityWeight: 12 },
        { id: 'traditional-center-right', leftPercent: 50, topPercent: 26, widthPercent: 44, heightPercent: 32, capacityWeight: 9 },
        { id: 'traditional-lower-right', leftPercent: 50, topPercent: 60, widthPercent: 44, heightPercent: 32, capacityWeight: 9 }
      ],
      exclusionZones: [
        { id: 'espresso-bottom-left', leftPercent: 0, topPercent: 54, widthPercent: 48, heightPercent: 46 },
        { id: 'beans-top-right', leftPercent: 64, topPercent: 0, widthPercent: 36, heightPercent: 24 }
      ]
    },
    COFFEE_YOUTHFUL_01: {
      templateId: 'COFFEE_YOUTHFUL_01',
      canvasWidth: 437,
      canvasHeight: 611,
      aspectRatio: 0.7152,
      blocks: [
        { id: 'youthful-top-center', leftPercent: 6, topPercent: 8, widthPercent: 54, heightPercent: 24, capacityWeight: 10 },
        { id: 'youthful-mid-right', leftPercent: 38, topPercent: 38, widthPercent: 56, heightPercent: 21, capacityWeight: 10 },
        { id: 'youthful-bottom-left', leftPercent: 6, topPercent: 66, widthPercent: 54, heightPercent: 24, capacityWeight: 10 }
      ],
      exclusionZones: [
        { id: 'matcha-glass-left', leftPercent: 0, topPercent: 34, widthPercent: 36, heightPercent: 32 },
        { id: 'boba-glass-top-right', leftPercent: 64, topPercent: 4, widthPercent: 36, heightPercent: 35 },
        { id: 'berry-glass-bottom-right', leftPercent: 64, topPercent: 61, widthPercent: 36, heightPercent: 35 }
      ]
    }
  };

  /**
   * Chế độ 1 Trang Duy Nhất (Single Page View Model):
   * Phẳng hóa toàn bộ món ăn theo thứ tự danh mục trong Blueprint thành 1 mảng sản phẩm duy nhất.
   */
  getSinglePageDishes(blueprint: VisualMenuBlueprint | null): ProductItemDto[] {
    if (!blueprint || !blueprint.sections || blueprint.sections.length === 0) {
      return [];
    }
    const allDishes: ProductItemDto[] = [];
    for (const sec of blueprint.sections) {
      if (sec.dishes && sec.dishes.length > 0) {
        for (const dish of sec.dishes) {
          allDishes.push(dish);
        }
      }
    }
    return allDishes;
  }

  /**
   * Tính toán trọng số tải (Weight / Height Estimation) của món ăn dựa trên độ dài tên sản phẩm:
   * Món tên ngắn (<= 20 ký tự) = 1 dòng (weight 1.0); tên dài (> 20 ký tự) = 2 dòng (weight 1.8).
   */
  estimateProductWeight(dish: ProductItemDto): number {
    if (!dish || !dish.name) return 1.0;
    const len = dish.name.trim().length;
    return len > 20 ? 1.8 : 1.0;
  }

  /**
   * Tính toán Font Size Class tự động dựa trên:
   * Dish count + average name length + total available block area.
   */
  calculateFontSizeClass(allDishes: ProductItemDto[], blocks: ContentBlockConfig[]): string {
    const totalCount = allDishes.length;
    if (totalCount === 0) return 'font-large';

    const totalWeight = allDishes.reduce((sum, dish) => sum + this.estimateProductWeight(dish), 0);
    const totalCapacity = blocks.reduce((sum, b) => sum + (b.capacityWeight || 10), 0);
    
    const loadRatio = totalWeight / totalCapacity;

    if (loadRatio <= 0.65 || totalCount <= 12) {
      return 'font-large';
    } else if (loadRatio <= 1.05 || totalCount <= 24) {
      return 'font-medium';
    } else {
      return 'font-compact';
    }
  }

  /**
   * Thuật toán Phân bổ Tải Cân Bằng (Weighted Balanced Load Distribution Engine):
   * Phân chia sản phẩm vào các Content Block cố định của template dựa trên tỷ lệ tải (usedWeight / capacityWeight).
   */
  buildSinglePageMenuView(blueprint: VisualMenuBlueprint | null, templateId: string): SinglePageMenuView {
    const template = this.getTemplateById(templateId);
    const key = (templateId || 'COFFEE_MODERN_01').toUpperCase() as CoffeeTemplateId;
    const layoutConfig = this.layoutConfigs[key] || this.layoutConfigs.COFFEE_MODERN_01;

    // Kiểm tra an toàn giao cắt trước khi render
    this.validateContentBlocksAgainstExclusionZones(layoutConfig);

    const allDishes = this.getSinglePageDishes(blueprint);
    const allocatedBlocks: BlockDishGroup[] = layoutConfig.blocks.map(b => ({
      config: b,
      dishes: [],
      usedWeight: 0
    }));

    const fontSizeClass = this.calculateFontSizeClass(allDishes, layoutConfig.blocks);

    if (allDishes.length === 0 || allocatedBlocks.length === 0) {
      return {
        templateId: template.templateId,
        template,
        allocatedBlocks,
        totalDishesCount: 0,
        fontSizeClass
      };
    }

    // Gán từng món ăn vào khối có tỷ lệ tải hiện tại (usedWeight / capacityWeight) thấp nhất
    for (const dish of allDishes) {
      const weight = this.estimateProductWeight(dish);
      
      let bestBlock = allocatedBlocks[0];
      let lowestRatio = bestBlock.usedWeight / (bestBlock.config.capacityWeight || 10);

      for (let i = 1; i < allocatedBlocks.length; i++) {
        const blk = allocatedBlocks[i];
        const ratio = blk.usedWeight / (blk.config.capacityWeight || 10);
        if (ratio < lowestRatio) {
          lowestRatio = ratio;
          bestBlock = blk;
        }
      }

      bestBlock.dishes.push(dish);
      bestBlock.usedWeight += weight;
    }

    return {
      templateId: template.templateId,
      template,
      allocatedBlocks,
      totalDishesCount: allDishes.length,
      fontSizeClass
    };
  }
}
