package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboPlacementDto {
    private String mainProductId;
    private String mainProductName;
    private String pairedProductId;
    private String pairedProductName;
    private String comboName;
    private double comboPrice;
    private String reason;
}
