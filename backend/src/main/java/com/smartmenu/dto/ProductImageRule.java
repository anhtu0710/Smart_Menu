package com.smartmenu.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRule {
    private Boolean heroImagePresence;
    private String imagePlacement; // LEFT, RIGHT, CENTER
    private Double imageSizeRatio;
    private String imageMasking;   // CIRCLE, ROUND_RECT, ORGANIC
    private String shadowStyle;    // SOFT_DROP, GOLD_GLOW, NATURAL
    private String lightingStyle;
    private String visualPriority; // FOOD_IMAGE_FIRST
}
