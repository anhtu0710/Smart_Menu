package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalMenuProductDto {
    private String productId;           // Stable ID e.g. "P001"
    private String categoryId;          // Category ID e.g. "CAT01"
    private String productName;
    private Long originalPrice;         // Currency in Long (VND integer)
    private Long recommendedPrice;
    private Long finalDisplayPrice;     // Final price decided by BusinessOptimizationService
    private String priceRecommendationReason;
    private String promoGift;           // Quà tặng ưu đãi đi kèm (hướng dương, bò khô...)
    private List<String> businessTags;  // e.g. ["BEST_SELLER", "HIGH_PROFIT"]
    private Integer businessPriority;   // 1..4
    private String placementGroup;      // "FEATURED" | "CORE" | "SECONDARY" | "EXCLUDED"
    private Integer displayPriority;
    private Integer displayOrder;       // Order within Category
    private Integer globalDisplayOrder; // Global order across entire menu
    private Boolean includeInFinalMenu; // true for FEATURED/CORE/SECONDARY, false for EXCLUDED
    private Integer salesQuantity;
    private Double profitMargin;
}
