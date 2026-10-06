package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LayoutConstraintDto {
    private Integer canvasPriority;
    private String sectionType;     // HERO_BANNER | PRODUCT_GRID | LIST
    private Integer columns;
    private Integer maxRows;
    private Double areaPercentage;
}
