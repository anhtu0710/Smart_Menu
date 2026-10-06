package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderManifestItemDto {
    private String productId;
    private String originalName;
    private String renderedName;
    private Double expectedPrice;
    private Double renderedPrice;
    private String blockId;
}
