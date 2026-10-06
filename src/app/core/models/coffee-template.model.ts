import { VisualMenuBlueprint, ProductItemDto, PriceRecommendationDto, ComboPlacementDto, BlueprintSectionDto } from './ai-menu.model';

export type CoffeeTemplateId = 'COFFEE_MODERN_01' | 'COFFEE_LUXURY_01' | 'COFFEE_TRADITIONAL_01' | 'COFFEE_YOUTHFUL_01';

export interface TemplateSlotArea {
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  maxItems?: number;
}

export interface CoffeeTemplateDefinition {
  templateId: CoffeeTemplateId;
  templateName: string;
  badgeText: string;
  description: string;
  backgroundAsset: string;
  thumbnailAsset: string;
  primaryColor: string;
  accentColor: string;
  textColor: string;
  cardBgColor: string;
  maxProductsPerPage: number;
  hasHeroSection: boolean;
  hasComboSection: boolean;
}

export interface ContentBlockConfig {
  id: string;
  leftPercent: number;
  topPercent: number;
  widthPercent: number;
  heightPercent: number;
  capacityWeight?: number;
}

export interface ExclusionZoneConfig {
  id: string;
  leftPercent: number;
  topPercent: number;
  widthPercent: number;
  heightPercent: number;
}

export interface TemplateLayoutConfig {
  templateId: CoffeeTemplateId;
  canvasWidth: number;
  canvasHeight: number;
  aspectRatio: number;
  blocks: ContentBlockConfig[];
  exclusionZones?: ExclusionZoneConfig[];
}

export interface BlockDishGroup {
  config: ContentBlockConfig;
  dishes: ProductItemDto[];
  usedWeight: number;
}

export interface SinglePageMenuView {
  templateId: CoffeeTemplateId;
  template: CoffeeTemplateDefinition;
  allocatedBlocks: BlockDishGroup[];
  totalDishesCount: number;
  fontSizeClass: string;
  isOverflowing?: boolean;
}

export interface TemplatePageDto {
  pageIndex: number;
  totalPages: number;
  template: CoffeeTemplateDefinition;
  sections: BlueprintSectionDto[];
  heroDishes: ProductItemDto[];
  structuredCombos: ComboPlacementDto[];
}
