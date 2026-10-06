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
public class CategoryPlanDto {
    private String categoryId;
    private String categoryName;
    private Integer categoryOrder;
    private Integer displayPriority;
    private Double displayWeight;
    private Integer maxProductsDisplay;

    // Category Layout Strategy
    private String displayPosition; // TOP | MIDDLE | BOTTOM
    private String layoutType;      // HERO_SECTION | FEATURED_GRID | STANDARD_LIST
    private LayoutConstraintDto layoutConstraint;

    private List<ProductPlanDto> products;
}
