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
public class BusinessOptimizationResultDto {

    private String sessionId;

    private List<ProductOptimizationDto> products;

    private List<PriceRecommendationDto> priceRecommendations;

    private List<ComboRecommendationDto> combos;

    private List<ComboPlacementDto> structuredCombos; // Backward compatibility

    private List<String> removeProductIds;

    private List<CategoryOrderDto> categoryOrder;
}
