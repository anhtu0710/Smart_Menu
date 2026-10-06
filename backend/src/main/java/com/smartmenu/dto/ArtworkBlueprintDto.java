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
public class ArtworkBlueprintDto {

    private String composition; // EDITORIAL_FOOD_POSTER, LUXURY_RESTAURANT, PLAYFUL_YOUTH, VINTAGE_MENU
    private List<ArtworkBlock> blocks;
    private String backgroundFill;
    private String borderStyle;
    private String primaryColor;
    private String accentColor;
    private String textColor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtworkBlock {
        private String type;     // TITLE, HERO_IMAGE, MENU_LIST, COMBO_PROMO, FEATURED_HIGHLIGHT
        private String position; // LEFT_TOP, RIGHT_TOP, CENTER_TOP, CENTER, BOTTOM, FULL_TOP
        private Integer priority;
        private String alignment; // LEFT, CENTER, RIGHT
        private Double heightRatio;
    }
}
