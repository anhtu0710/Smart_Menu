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
public class FinalMenuPlacementStrategyDto {
    private List<String> featuredProductIds;
    private List<String> coreProductIds;
    private List<String> secondaryProductIds;
    private List<String> excludedProductIds;
    private List<String> rules;
}
