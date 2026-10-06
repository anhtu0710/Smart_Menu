package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FnbRuleDto {
    private String id;
    private String ruleName;
    private String category;
    private String description;
    private String impactLevel;
}
