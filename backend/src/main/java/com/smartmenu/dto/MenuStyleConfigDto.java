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
public class MenuStyleConfigDto {
    private String styleId;
    private String styleName;
    private String referenceImagePath;          // e.g. "classpath:/menu-style/HIEN_DAI/reference.png"
    private List<String> referenceImages;
    private List<String> referenceImagePaths;   // Alias for referenceImages
    private String visualPrompt;                // e.g. "Modern coffee shop menu..."
    private String styleGoal;                   // e.g. "Highlight best sellers and high-profit drinks with clean modern aesthetic"
    private String compositionRule;             // e.g. "Asymmetrical modern header..."
    private String typographyRule;              // e.g. "SansSerif sleek modern typography..."
    private String colorRule;                   // e.g. "Dark Slate Navy canvas #0f172a..."
    private String decorationRule;              // e.g. "Subtle glowing cyan accent bars..."
    private String imageMoodRule;               // e.g. "Realistic F&B food photography with high contrast studio lighting"

    private String theme;
    private List<String> colorPalette;
    private String fontStyle;
    private String backgroundStyle;
    private String imageStyle;
    private String decorationLevel;
    private String assetMode;
}
