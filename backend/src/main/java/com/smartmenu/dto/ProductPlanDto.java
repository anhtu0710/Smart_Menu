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
public class ProductPlanDto {
    private String productId;
    private String categoryId;
    private String productName;
    private Double finalDisplayPrice;
    private List<String> businessTags;

    // 1. Business Decision Layer (From BusinessOptimizationService)
    private String decision;        // PRICE_INCREASE | KEEP | COMBO | REMOVE
    private Double originalPrice;
    private Double recommendedPrice;
    private Double profitMargin;
    private Integer salesQuantity;
    private String promoGift;       // Quà tặng ưu đãi đi kèm (hướng dương, bò khô...)

    // 2. Display Decision Layer (From FinalMenuPlanService)
    private String displayGroup;    // HERO | FEATURED | CORE | COMBO
    private String displaySize;     // LARGE | MEDIUM | STANDARD
    private String displayPosition; // TOP | MIDDLE | BOTTOM
    private String displayPurpose;  // MAIN_ATTENTION | PROMINENT_CARD | STANDARD_LIST
    private LayoutConstraintDto layoutConstraint;

    private Integer displayPriority;
    private Integer displayOrder;
    private Integer globalDisplayOrder;
    private Boolean includeInFinalMenu;

    public Double getFinalPrice() {
        return finalDisplayPrice;
    }
}
