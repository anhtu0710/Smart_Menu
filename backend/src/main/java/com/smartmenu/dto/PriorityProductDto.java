package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorityProductDto {
    private String productId;
    private String productName;
    private String category;
    private String priorityType; // CORE | HERO | BEST_SELLER | HIGH_PROFIT | CROSS_SELL
    private String reason;
}
