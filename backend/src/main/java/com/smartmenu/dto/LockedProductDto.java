package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockedProductDto {
    private String id;
    private String name;
    private Double price;
    private String formattedPrice;
    private Integer order;
    private String categoryName;
    private Double originalPrice;
    private Double recommendedPrice;
}
