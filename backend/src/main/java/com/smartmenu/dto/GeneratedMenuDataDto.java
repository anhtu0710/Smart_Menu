package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMenuDataDto {
    private Long sessionId;
    @Builder.Default
    private String selectedStyleId = "COFFEE_MODERN_01";
    @Builder.Default
    private int blueprintVersion = 1;
    private VisualMenuBlueprintDto blueprint;
    private FinalMenuPlanDto finalMenuPlan;
}

