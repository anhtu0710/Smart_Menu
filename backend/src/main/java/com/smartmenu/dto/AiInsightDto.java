package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightDto {
    private String id;
    private String type; // REVENUE, PROFIT, CATEGORY, LAYOUT
    private String title;
    private String evidence;
    private String menuObservation;
    private String conclusion;
}
