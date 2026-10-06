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
public class AiStrategyDto {
    private String id;
    private List<String> basedOnInsightIds;
    private String action;
    private String reason;
    private String expectedImpact;
}
