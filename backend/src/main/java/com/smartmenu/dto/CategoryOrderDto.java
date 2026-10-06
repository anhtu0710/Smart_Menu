package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryOrderDto {

    private String categoryId;

    private String categoryName;

    private Integer categoryOrder;

    private Double categoryRevenue;

    private Double categoryProfit;

    private Double priorityScore;
}
