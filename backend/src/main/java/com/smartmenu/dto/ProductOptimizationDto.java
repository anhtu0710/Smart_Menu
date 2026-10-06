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
public class ProductOptimizationDto {

    private String productId;

    private String productName;

    private String categoryId;

    private String categoryName;

    private List<String> businessTags;

    private String decision; // KEEP, PRICE_INCREASE, COMBO, REDUCE_PRIORITY, REMOVE

    private Long originalPrice;

    private Long recommendedPrice;

    private Long finalDisplayPrice;

    private Boolean includeInFinalMenu;

    private Integer businessPriority; // 1 = HIGH_PROFIT + BEST_SELLER, 2 = HIGH_PROFIT, 3 = BEST_SELLER, 4 = CORE_PRODUCT

    private Integer categoryOrder;

    private Integer displayOrder;

    private Integer globalDisplayOrder;

    private String reason;
    private String promoGift; // Quà tặng ưu đãi đi kèm (hướng dương, bò khô...)

    // Financial metrics for traceability
    private double profitMargin;

    private int salesQuantity;

    private double totalRevenue;

    private double estimatedProfit;

    private String bcgCategory;
}
