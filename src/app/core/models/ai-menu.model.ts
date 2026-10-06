export interface UploadResponseDto {
  sessionId: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  filePath: string;
  fileCategory: string;
}

export interface AnalysisReportDto {
  totalRevenue: number;
  totalItemsSold: number;
  estimatedProfit: number;
  averageMargin: number;
  optimizationScore: number;
  bestSellersText?: string;
  highProfitText?: string;
  lowPriorityText?: string;
  heroDishesText?: string;
  suggestedCombosText?: string;
}

export interface ProductItemDto {
  id?: string;
  name: string;
  category?: string;
  originalPrice: number;
  costPrice?: number;
  salesQuantity: number;
  totalRevenue?: number;
  profitMargin?: number;
  bcgCategory?: string; // 'STAR' | 'PUZZLE' | 'CASH_COW' | 'DOG'
}

export interface CategoryRevenueDto {
  name: string;
  revenue: number;
  percentage: number;
  itemQuantity?: number;
}

export interface RecommendationDto {
  id?: string;
  type?: string;
  title: string;
  description: string;
  priority?: string;
  impact?: string;
}

export interface FnbRuleDto {
  id?: string;
  ruleName: string;
  category?: string;
  description: string;
  impactLevel?: string;
}

export interface PriceRecommendationDto {
  productId: string;
  productName: string;
  categoryName: string;
  currentPrice: number;
  recommendedPrice: number;
  priceChangePercent: number;
  reason: string;
}

export interface PriorityProductDto {
  productId?: string;
  productName: string;
  category?: string;
  priorityType?: 'CORE' | 'HERO' | 'BEST_SELLER' | 'HIGH_PROFIT' | 'CROSS_SELL' | string;
  reason?: string;
}

export interface ComboPlacementDto {
  mainProductId: string;
  mainProductName: string;
  pairedProductId: string;
  pairedProductName: string;
  comboName: string;
  comboPrice: number;
  reason: string;
}

export interface MenuRestructurePlanDto {
  coreDishes?: string[];
  heroDishes?: string[];
  priorityProducts?: PriorityProductDto[];
  categoryOrder?: string[];
  reducedPriorityItems?: string[];
  comboPlacements?: string[];
  structuredCombos?: ComboPlacementDto[];
  priceRecommendations?: PriceRecommendationDto[];
  imagePriority?: string[];
  topZone?: string;
  centerZone?: string;
  secondaryZone?: string;
  finalLayoutSummary?: string;
}

export interface FullAnalysisResponseData {
  sessionId: string;
  analysis: AnalysisReportDto;
  products: ProductItemDto[];
  categoryRevenue?: CategoryRevenueDto[];
  recommendations?: RecommendationDto[];
  fnbRules?: FnbRuleDto[];
  reasoning: string;
  menuVisionIssues?: string;
  restructurePlan?: MenuRestructurePlanDto;
}

export interface GeneratedMenu {
  id?: number;
  sessionId: number;
  menuTitle?: string;
  themeStyle?: string;
  menuContentJson?: string;
  menuImageUrl?: string;
  pdfExportUrl?: string;
  status?: string;
}

export interface AIAnalysisResult {
  id?: number;
  sessionId: number;
  analysisSummary?: string;
  menuScore?: number;
  bcgMatrixJson?: string;
  fnbRulesJson?: string;
  menuStrategyJson?: string;
  geminiReasoning?: string;
  visionAnalysisResult?: string;
}

export interface BlueprintSectionDto {
  categoryName: string;
  priority: 'HERO' | 'CORE' | 'NORMAL' | 'REDUCED';
  dishes: ProductItemDto[];
}

export interface VisualMenuBlueprint {
  version: number;
  sessionId?: number;
  brandName?: string;
  logoUrl?: string;
  tagline?: string;
  heroDishes: ProductItemDto[];
  coreDishes: ProductItemDto[];
  priorityProducts?: PriorityProductDto[];
  categoryOrder: string[];
  reducedPriorityItems: ProductItemDto[];
  comboPlacements: string[];
  structuredCombos?: ComboPlacementDto[];
  priceRecommendations?: PriceRecommendationDto[];
  imagePriority: ProductItemDto[];
  sections: BlueprintSectionDto[];
  layoutSummary: string;
}

export interface GeneratedMenuDataDto {
  sessionId: number;
  selectedStyleId: 'MODERN' | 'MINIMAL' | 'ELEGANT' | 'TRADITIONAL' | 'YOUTHFUL' | string;
  blueprintVersion: number;
  blueprint: VisualMenuBlueprint;
}

export interface MenuStyleOption {
  styleId: 'MODERN' | 'MINIMAL' | 'ELEGANT' | 'TRADITIONAL' | 'YOUTHFUL';
  styleName: string;
  description: string;
  badgeText: string;
  primaryColor: string;
}

// Giữ lại các kiểu tiện ích UI (alias/legacy nếu có component khác import)
export type ProductItem = ProductItemDto;
export type AnalysisReport = AnalysisReportDto;
export type FnbRuleItem = FnbRuleDto;
export type RecommendationItem = RecommendationDto;
