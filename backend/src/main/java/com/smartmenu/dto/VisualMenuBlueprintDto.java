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
public class VisualMenuBlueprintDto {
    @Builder.Default
    private int version = 1;
    private Long sessionId;
    private String brandName;
    private String logoUrl;
    private String tagline;
    private List<ProductItemDto> heroDishes;
    private List<ProductItemDto> coreDishes;
    private List<PriorityProductDto> priorityProducts;
    private List<String> categoryOrder;
    private List<ProductItemDto> reducedPriorityItems;
    private List<String> comboPlacements;
    private List<ComboPlacementDto> structuredCombos;
    private List<PriceRecommendationDto> priceRecommendations;
    private List<ProductItemDto> imagePriority;
    private List<BlueprintSectionDto> sections;
    private String layoutSummary;
}

