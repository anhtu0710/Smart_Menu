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
public class FullAnalysisResponseData {
    private String sessionId;
    private AnalysisReportDto analysis;
    private List<ProductItemDto> products;
    private List<CategoryRevenueDto> categoryRevenue;
    private List<RecommendationDto> recommendations;
    private List<FnbRuleDto> fnbRules;
    private String reasoning;
    private String menuVisionIssues;

    // Kiến trúc AI & Optimization Phase 2
    private List<AiInsightDto> insights;
    private List<AiStrategyDto> strategies;
    private MenuRestructurePlanDto restructurePlan;
    private BusinessOptimizationResultDto optimizationResult;
    private FinalMenuPlanDto finalMenuPlan;
}
