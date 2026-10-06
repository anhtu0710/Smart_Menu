package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisReportDto {
    private double totalRevenue;
    private int totalItemsSold;
    private double estimatedProfit;
    private double averageMargin;
    private int optimizationScore;
    private String bestSellersText;
    private String highProfitText;
    private String lowPriorityText;
    private String heroDishesText;
    private String suggestedCombosText;
}
