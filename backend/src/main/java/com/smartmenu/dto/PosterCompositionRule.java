package com.smartmenu.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosterCompositionRule {
    private Integer heroSectionHeight; // e.g., 250px
    private Double imageAreaRatio;     // e.g., 0.40
    private Integer menuStartY;         // e.g., 380px
    private Double whiteSpaceRatio;    // e.g., 0.30
    private String productDensity;     // SPACIOUS, BALANCED, COMPACT
    private String titleBlockPosition; // LEFT, CENTER, RIGHT
}
