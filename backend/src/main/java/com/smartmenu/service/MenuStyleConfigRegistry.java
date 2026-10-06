package com.smartmenu.service;

import com.smartmenu.dto.MenuStyleConfigDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MenuStyleConfigRegistry {

    private final Map<String, MenuStyleConfigDto> registry = new HashMap<>();

    public MenuStyleConfigRegistry() {
        initRegistry();
    }

    private void initRegistry() {
        // 1. HIEN_DAI / MODERN
        registry.put("HIEN_DAI", MenuStyleConfigDto.builder()
                .styleId("HIEN_DAI")
                .styleName("Hiện Đại Thương Mại")
                .referenceImagePath("classpath:/menu-style/HIEN_DAI/reference.png")
                .referenceImages(List.of("classpath:/menu-style/HIEN_DAI/reference.png"))
                .referenceImagePaths(List.of("classpath:/menu-style/HIEN_DAI/reference.png"))
                .visualPrompt(
                        "Modern coffee shop menu, clean commercial poster layout, Slate Navy background, cyan glowing accents, professional F&B typography, balanced whitespace")
                .styleGoal("Highlight best sellers and high-profit drinks with clean modern commercial aesthetic")
                .typographyRule("SansSerif sleek modern typography with high contrast price tags")
                .colorRule("Dark Slate Navy canvas #0f172a with electric cyan #38bdf8 accents and amber #f59e0b prices")
                .decorationRule("Subtle glowing cyan accent bars and clean geometric divider lines")
                .compositionRule(
                        "Asymmetrical modern header, large HERO visual banner area, clean CORE text list, promo combo section at bottom")
                .imageMoodRule("High contrast luxury F&B food visual with neon cyan glowing accents")
                .theme("MODERN_SLATE")
                .colorPalette(
                        List.of("#0f172a", "#38bdf8", "#f8fafc", "#f59e0b", "#0284c7", "#1e293b", "#0f172a", "#1e1b4b"))
                .fontStyle("SansSerif")
                .backgroundStyle("DARK_NAVY_SLATE")
                .imageStyle("VECTOR_ILLUSTRATION")
                .decorationLevel("MEDIUM")
                .assetMode("PURE_SVG_DYNAMIC")
                .build());

        registry.put("MODERN", registry.get("HIEN_DAI"));
        registry.put("COFFEE_MODERN_01", registry.get("HIEN_DAI"));

        // 2. SANG_TRONG / LUXURY
        registry.put("SANG_TRONG", MenuStyleConfigDto.builder()
                .styleId("SANG_TRONG")
                .styleName("Sang Trọng Hoàng Gia")
                .referenceImagePath("classpath:/menu-style/SANG_TRONG/reference.png")
                .referenceImages(List.of("classpath:/menu-style/SANG_TRONG/reference.png"))
                .referenceImagePaths(List.of("classpath:/menu-style/SANG_TRONG/reference.png"))
                .visualPrompt(
                        "Luxury high-end restaurant menu, dark espresso background, gold 24k typography, royal double border frame, crown crest ornaments, premium fine dining feeling")
                .styleGoal("Create an elegant high-end fine dining luxury restaurant menu")
                .typographyRule("Elegant Serif typography with royal crown header alignment")
                .colorRule("Espresso dark canvas #1c1917 with gold foil #fbbf24 accents and amber #f59e0b highlights")
                .decorationRule("Royal gold metallic double border, crown crest divider, gold star accents")
                .compositionRule(
                        "Centered luxury layout, prominent HERO gold banner, subtle featured cards, elegant core text rows")
                .imageMoodRule("Royal studio food photography with warm gold highlights")
                .theme("LUXURY_ELEGANT")
                .colorPalette(
                        List.of("#1c1917", "#f59e0b", "#fef3c7", "#fbbf24", "#b45309", "#292524", "#1c1917", "#451a03"))
                .fontStyle("Serif")
                .backgroundStyle("DARK_GOLD_LUXURY")
                .imageStyle("VECTOR_ILLUSTRATION")
                .decorationLevel("HIGH")
                .assetMode("PURE_SVG_DYNAMIC")
                .build());

        registry.put("LUXURY", registry.get("SANG_TRONG"));
        registry.put("COFFEE_LUXURY_01", registry.get("SANG_TRONG"));

        // 3. TRUYEN_THONG / VINTAGE
        registry.put("TRUYEN_THONG", MenuStyleConfigDto.builder()
                .styleId("TRUYEN_THONG")
                .styleName("Truyền Thống Cổ Điển")
                .referenceImagePath("classpath:/menu-style/TRUYEN_THONG/reference.png")
                .referenceImages(List.of("classpath:/menu-style/TRUYEN_THONG/reference.png"))
                .referenceImagePaths(List.of("classpath:/menu-style/TRUYEN_THONG/reference.png"))
                .visualPrompt(
                        "Traditional Vietnamese coffee house menu, warm parchment paper background, retro coffee house banner, sepia brown tones, rustic classic atmosphere")
                .styleGoal("Create a vintage classic Vietnamese coffee house parchment menu")
                .typographyRule("Classic Retro Serif typography with warm brown price highlights")
                .colorRule("Warm parchment canvas #fbf0d9 with sepia brown #78350f headers and amber #b45309 accents")
                .decorationRule("Retro double border frame, vintage corner ornaments, coffee bean graphic accents")
                .compositionRule("Classic retro banner header, prominent HERO visual section, clean vintage text list")
                .imageMoodRule("Warm rustic retro food visual on parchment canvas")
                .theme("VINTAGE_CLASSIC")
                .colorPalette(
                        List.of("#fbf0d9", "#78350f", "#451a03", "#b45309", "#d97706", "#fef3c7", "#fbf0d9", "#7c2d12"))
                .fontStyle("Serif")
                .backgroundStyle("WARM_VINTAGE_PARCHMENT")
                .imageStyle("LINE_ART")
                .decorationLevel("HIGH")
                .assetMode("PURE_SVG_DYNAMIC")
                .build());

        registry.put("VINTAGE", registry.get("TRUYEN_THONG"));
        registry.put("COFFEE_TRADITIONAL_01", registry.get("TRUYEN_THONG"));

        // 4. TRE_TRUNG / CUTE
        registry.put("TRE_TRUNG", MenuStyleConfigDto.builder()
                .styleId("TRE_TRUNG")
                .styleName("Trẻ Trung Pastel")
                .referenceImagePath("classpath:/menu-style/TRE_TRUNG/reference.png")
                .referenceImages(List.of("classpath:/menu-style/TRE_TRUNG/reference.png"))
                .referenceImagePaths(List.of("classpath:/menu-style/TRE_TRUNG/reference.png"))
                .visualPrompt(
                        "Youthful cafe social media style menu, soft pastel pink background, cute rounded pill header, friendly rounded typography, soft pastel cards, playful star accents")
                .styleGoal("Create a playful cute youth cafe menu for social media generation")
                .typographyRule("Friendly rounded SansSerif typography with rose pink price highlights")
                .colorRule(
                        "Soft pastel pink canvas #fff0f5 with deep rose #db2777 headers and soft pink #ffe4e1 card fills")
                .decorationRule("Cute rounded corners rx=32px, pill header rx=26px, star/circle decorative accents")
                .compositionRule("Rounded pill header, cute HERO card container, friendly core text list")
                .imageMoodRule("Soft pastel food photography with warm natural lighting")
                .theme("CUTE_PASTEL")
                .colorPalette(
                        List.of("#fff0f5", "#db2777", "#831843", "#be185d", "#fbcfe8", "#ffe4e1", "#fff0f5", "#fce7f3"))
                .fontStyle("DejaVu Sans")
                .backgroundStyle("PASTEL_CUTE_PINK")
                .imageStyle("VECTOR_ILLUSTRATION")
                .decorationLevel("HIGH")
                .assetMode("PURE_SVG_DYNAMIC")
                .build());

        registry.put("CUTE", registry.get("TRE_TRUNG"));
        registry.put("COFFEE_YOUTHFUL_01", registry.get("TRE_TRUNG"));

        // 5. MINIMAL
        registry.put("MINIMAL", MenuStyleConfigDto.builder()
                .styleId("MINIMAL")
                .styleName("Tối Giản Tinh Tế")
                .referenceImagePath("classpath:/menu-style/HIEN_DAI/reference.png")
                .referenceImages(List.of("classpath:/menu-style/HIEN_DAI/reference.png"))
                .referenceImagePaths(List.of("classpath:/menu-style/HIEN_DAI/reference.png"))
                .visualPrompt("Minimalist clean white menu, monochrome slate typography, thin subtle line dividers")
                .styleGoal("Create an ultra-minimal clean white modern layout")
                .typographyRule("Ultra-clean SansSerif typography")
                .colorRule("Clean white canvas #fafafa with slate #0f172a typography")
                .decorationRule("Thin stroke line frame, minimal layout clutter")
                .compositionRule("Minimal centered list layout, generous whitespace")
                .imageMoodRule("Clean studio food portrait")
                .theme("MINIMAL_CLEAN")
                .colorPalette(
                        List.of("#fafafa", "#0f172a", "#334155", "#0f172a", "#e2e8f0", "#ffffff", "#fafafa", "#f1f5f9"))
                .fontStyle("SansSerif")
                .backgroundStyle("CLEAN_MINIMAL_WHITE")
                .imageStyle("LINE_ART")
                .decorationLevel("MINIMAL")
                .assetMode("PURE_SVG_DYNAMIC")
                .build());
    }

    public MenuStyleConfigDto getStyleConfig(String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return registry.get("HIEN_DAI");
        }
        String key = styleId.toUpperCase().trim();
        return registry.getOrDefault(key, registry.get("HIEN_DAI"));
    }
}
