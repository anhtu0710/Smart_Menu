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
public class DesignSpecificationDto {

    private String styleId;
    private String compositionStyle; // EDITORIAL_FOOD_POSTER, LUXURY_RESTAURANT, MODERN_CAFE, VINTAGE_MENU, PLAYFUL_YOUTH
    private List<String> visualHierarchy; // FOOD_IMAGE, BRAND_TITLE, HERO_PRODUCT, PRICE, MENU_LIST
    private HeroStrategy heroStrategy;
    private MenuStrategy menuStrategy;
    private TypographySpec typography;
    private DecorationSpec decoration;
    private LayoutIntent layoutIntent;
    private ArtworkBlueprintDto artworkBlueprint;
    private boolean referenceImageUnderstood;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeroStrategy {
        private boolean enabled;
        private double imageDominance;
        private String heroPosition;
        private String heroLayout;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuStrategy {
        private String productDensity;
        private boolean showCards;
        private boolean showBorders;
        private String productArrangement;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypographySpec {
        private String style;
        private String priceEmphasis;
        private String fontFamily;
        private Integer titleFontSize;
        private Integer categoryFontSize;
        private Integer productFontSize;
        private Integer priceFontSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecorationSpec {
        private String decorationType;
        private String dividerStyle;
        private boolean cornerAccents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayoutIntent {
        private String compositionFlow; // FOOD_FIRST, TITLE_FIRST, BRAND_FIRST
        private String productArrangement; // EDITORIAL_LIST, MAGAZINE_LAYOUT, TWO_COLUMN_MENU, PREMIUM_SINGLE_COLUMN
        private String whitespaceStyle; // LUXURY_SPACIOUS, MODERN_CLEAN, COMPACT_DYNAMIC
        private String imageComposition; // FULL_BLEED, FLOATING_FOOD, HERO_CENTER
    }
}
