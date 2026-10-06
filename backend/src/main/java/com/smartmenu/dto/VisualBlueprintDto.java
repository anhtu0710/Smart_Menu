package com.smartmenu.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualBlueprintDto {
    private String styleId;

    // Canvas
    private Integer canvasWidth;
    private Integer canvasHeight;
    private Integer paddingX;
    private Integer paddingY;
    private String backgroundStyle;
    private String backgroundColor;
    private String backgroundGradient;

    // Header
    private Integer headerHeight;
    private String headerAlignment;
    private String headerDecorationType;
    private String titleStyle;

    // Typography
    private String fontFamily;
    private Integer titleFontSize;
    private Integer categoryFontSize;
    private Integer productFontSize;
    private Integer priceFontSize;
    private String typographyHierarchy;

    // Layout Composition
    private String layoutComposition;
    private String whitespaceStrategy;
    private String visualHierarchy;

    // Product Arrangement
    private String productArrangement;
    private Integer columnsCount;

    // Decoration
    private String decorationRule;
    private String dividerStyle;

    // Product Image Rule
    private ProductImageRule productImageRule;

    // Poster Composition Rule
    private PosterCompositionRule posterCompositionRule;

    // Audit
    private Boolean referenceApplied;
}
