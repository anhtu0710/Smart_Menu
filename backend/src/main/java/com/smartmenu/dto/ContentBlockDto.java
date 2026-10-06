package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlockDto {
    private String id;
    private double leftPercent;
    private double topPercent;
    private double widthPercent;
    private double heightPercent;
    private double capacityWeight;
}
