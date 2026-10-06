package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductItemDto {
    private String id;
    private String name;
    private String category;
    private double originalPrice;
    private double costPrice;
    private int salesQuantity;
    private double totalRevenue;
    private double profitMargin;
    private String bcgCategory; // STAR, PUZZLE, CASH_COW, DOG
}
