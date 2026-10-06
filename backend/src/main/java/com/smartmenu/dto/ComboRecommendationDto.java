package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboRecommendationDto {

    private String comboId;

    private String mainProductId;

    private String mainProductName;

    private String secondaryProductId;

    private String secondaryProductName;

    private String comboName;

    private Long comboPrice;

    private String reason;
}
