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
public class DisplayStrategyDto {
    private List<String> categoryOrder;
    private List<String> heroProductIds;
    private List<String> featuredProductIds;
    private List<String> coreProductIds;
    private List<String> comboIds;
    private List<String> excludedProductIds;
    private List<String> rules;
}
