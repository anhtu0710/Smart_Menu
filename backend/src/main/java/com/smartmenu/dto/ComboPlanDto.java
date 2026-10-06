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
public class ComboPlanDto {
    private String comboId;
    private String comboName;
    private Double comboPrice;
    private String reason;

    // Embedded Real Products (Full objects, no lookup needed)
    private List<String> productIds;
    private List<ProductPlanDto> products;

    // Combo Display Strategy
    private String displayGroup;    // COMBO_HIGHLIGHT
    private String displaySize;     // MEDIUM
    private String displayPosition; // SECONDARY_ZONE
    private LayoutConstraintDto layoutConstraint;

    private Integer displayOrder;
}
