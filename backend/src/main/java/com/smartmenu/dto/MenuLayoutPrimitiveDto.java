package com.smartmenu.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuLayoutPrimitiveDto {
    private CanvasPrimitive canvas;
    private HeaderPrimitive header;
    private HeroPrimitive hero;
    private MenuListPrimitive menuList;
    private List<CategoryPrimitive> categories;
    private ComboPrimitive combo;

    // Primitive Detail Summary for Audit Logging
    private String layoutComposition;
    private String headerAlignment;
    private String heroPosition;
    private String heroSize;
    private String productArrangement;
    private Integer columns;
    private String whitespace;
    private String foodVisualPriority;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CanvasPrimitive {
        private int width;
        private int height;
        private int paddingX;
        private int paddingY;
        private String fill;
        private String stroke;
        private int strokeWidth;
        private String dashArray;
        private String layoutType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeaderPrimitive {
        private String title;
        private int x;
        private int y;
        private int height;
        private String alignment;
        private String fontFamily;
        private int fontSize;
        private String fill;
        private String decorationType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeroImagePrimitive {
        private String productId;
        private String foodImageUrl;
        private int x;
        private int y;
        private int width;
        private int height;
        private String imagePlacement; // LEFT, RIGHT, CENTER
        private String imageShape;     // CIRCLE, ROUND_RECT, ORGANIC
        private String shadowStyle;    // GOLD_GLOW, SOFT_DROP, NEON_GLOW
        private String lightingStyle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TitleBlockPrimitive {
        private String productId;
        private String titleText;
        private String subtitleText;
        private String priceText;
        private int x;
        private int y;
        private int width;
        private int height;
        private String alignment;
        private String fontFamily;
        private int titleFontSize;
        private int priceFontSize;
        private String titleFill;
        private String priceFill;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeroPrimitive {
        private String productId;
        private String categoryId;
        private String displayGroup;
        private String displaySize;
        private String displayPosition;
        private String placementGroup;
        private int displayPriority;
        private int x;
        private int y;
        private int width;
        private int height;
        private HeroImagePrimitive imagePrimitive;
        private TitleBlockPrimitive titleBlockPrimitive;
        private String heroBgFill;
        private String heroStroke;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuListPrimitive {
        private int startY;
        private int categoryGap;
        private int itemGap;
        private boolean typographyOnlyList; // ZERO cards, ZERO borders
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryPrimitive {
        private String categoryId;
        private String categoryName;
        private int categoryOrder;
        private double displayWeight;
        private String layoutType;
        private int x;
        private int y;
        private int width;
        private int height;
        private String dividerStyle;
        private String headerFill;
        private List<ProductPrimitive> products;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPrimitive {
        private String productId;
        private String categoryId;
        private String displayGroup;
        private String displaySize;
        private String displayPosition;
        private String displayPurpose;
        private String placementGroup;
        private int displayPriority;
        private int x;
        private int y;
        private int width;
        private int height;
        private int nameX;
        private int nameY;
        private int priceX;
        private int priceY;
        private int fontSize;
        private String nameFill;
        private String priceFill;
        private boolean showCardBox;
        private boolean showDottedLine;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboPrimitive {
        private int x;
        private int y;
        private int width;
        private int height;
        private String bgFill;
        private String stroke;
        private List<ComboItemPrimitive> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboItemPrimitive {
        private String comboId;
        private String comboName;
        private double comboPrice;
        private int x;
        private int y;
    }
}
