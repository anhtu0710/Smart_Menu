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
public class FinalMenuPlanDto {
    private String sessionId;
    private Integer planVersion;
    private String planHash;
    private String createdAt;

    private List<CategoryPlanDto> categories;
    private List<ComboPlanDto> combos;
    private List<String> excludedProductIds;
    private DisplayStrategyDto displayStrategy;
    private AnalysisReportDto analysisSummary;

    // Backward Compatibility fields
    private FinalMenuContentDto finalMenuContent;
    private FinalMenuPlacementStrategyDto placementStrategy;
}
