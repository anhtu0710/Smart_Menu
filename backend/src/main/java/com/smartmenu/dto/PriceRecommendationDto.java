package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceRecommendationDto {
    private String productId;
    private String productName;
    private String categoryName;
    private double currentPrice;
    private double recommendedPrice;
    private double priceChangePercent;
    private String reason;
}
