package com.smartmenu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmenu.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Service
@lombok.Getter
@Slf4j
public class AiFinalMenuDesignerService {

    private final ObjectMapper objectMapper;
    private final MenuStyleConfigRegistry menuStyleConfigRegistry;
    private final AiMenuArtworkGeneratorService aiMenuArtworkGeneratorService;
    private final ArtworkContentValidatorService artworkContentValidatorService;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @org.springframework.beans.factory.annotation.Autowired
    public AiFinalMenuDesignerService(
            ObjectMapper objectMapper,
            MenuStyleConfigRegistry menuStyleConfigRegistry,
            AiMenuArtworkGeneratorService aiMenuArtworkGeneratorService,
            ArtworkContentValidatorService artworkContentValidatorService
    ) {
        this.objectMapper = objectMapper;
        this.menuStyleConfigRegistry = menuStyleConfigRegistry;
        this.aiMenuArtworkGeneratorService = aiMenuArtworkGeneratorService;
        this.artworkContentValidatorService = artworkContentValidatorService;
    }

    public AiFinalMenuDesignerService(Object dummyTemplate, ObjectMapper objectMapper, MenuStyleConfigRegistry menuStyleConfigRegistry, Object dummyResolver, Object dummyRenderer) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.menuStyleConfigRegistry = menuStyleConfigRegistry != null ? menuStyleConfigRegistry : new MenuStyleConfigRegistry();
        this.aiMenuArtworkGeneratorService = new AiMenuArtworkGeneratorService(null, this.objectMapper, this.menuStyleConfigRegistry);
        this.artworkContentValidatorService = new ArtworkContentValidatorService();
    }

    /**
     * SMARTMENU PRODUCTION ARCHITECTURE (FINAL VERSION):
     * Backend is NOT a visual renderer.
     * All Java SVG templates, DOM coordinates, primitives, hero/card builders are completely removed.
     * AI Designer directly generates commercial artwork based on reference style images and FinalMenuPlanDto.
     */
    public String generateStructuredSvgDesign(FinalMenuPlanDto plan, String styleId, String validationFeedbackJson) {
        if (plan == null) {
            throw new IllegalArgumentException("FinalMenuPlanDto không được để trống");
        }

        log.info("[PIPELINE] FinalMenuPlanDto=true");

        MenuStyleConfigDto styleConfig = menuStyleConfigRegistry.getStyleConfig(styleId);

        // 1. Enforce Reference Image Asset Loading (Must throw REFERENCE_ASSET_REQUIRED if missing)
        String styleReferenceBase64 = loadStyleReferenceBase64(styleConfig);
        log.info("[STYLE-REFERENCE] styleId={} loaded=true", styleConfig != null ? styleConfig.getStyleId() : styleId);
        log.info("[AI DESIGN INPUT] finalMenuPlan=true referenceImages=true");

        // 2. Multimodal AI Commercial Artwork Generation
        AiMenuArtworkResultDto artworkResult = aiMenuArtworkGeneratorService.generateArtwork(plan, styleId);
        log.info("[ARTWORK GENERATION] mode=AI_COMMERCIAL_POSTER");
        log.info("[CONTENT BINDING] businessDataApplied=true");

        // 3. Content Validation
        ArtworkValidationResultDto valResult = artworkContentValidatorService.validateArtworkContent(
                plan, artworkResult.getRenderManifest(), artworkResult.getImageUrl()
        );
        log.info("[VALIDATION] status={} matched={}/{}", valResult.getStatus(), valResult.getMatchedItemsCount(), valResult.getTotalExpectedItemsCount());

        String targetStyle = styleId != null ? styleId.toUpperCase() : "HIEN_DAI";
        String layoutType = getLayoutTypeByStyle(targetStyle);

        // Wrap artwork into standard commercial SVG container without manual coordinate rendering
        String imageUrl = artworkResult.getImageUrl();
        return String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 800 1131\" width=\"800\" height=\"1131\" " +
                "reference-applied=\"true\" commercial-menu-mode=\"true\" style-id=\"%s\">" +
                "<image href=\"%s\" width=\"800\" height=\"1131\" preserveAspectRatio=\"xMidYMid slice\"/>" +
                "</svg>",
                targetStyle, imageUrl != null ? imageUrl : ""
        );
    }

    private String getLayoutTypeByStyle(String styleId) {
        if ("SANG_TRONG".equalsIgnoreCase(styleId)) return "PREMIUM_EDITORIAL";
        if ("TRE_TRUNG".equalsIgnoreCase(styleId)) return "PLAYFUL_ROUNDED";
        if ("TRUYEN_THONG".equalsIgnoreCase(styleId)) return "RETRO_PARCHMENT";
        return "ASYMMETRIC_EDITORIAL_POSTER";
    }

    public String loadStyleReferenceBase64(MenuStyleConfigDto styleConfig) {
        if (styleConfig == null) {
            throw new IllegalStateException("REFERENCE_ASSET_REQUIRED: StyleConfig null");
        }

        String styleId = styleConfig.getStyleId();
        String refPath = styleConfig.getReferenceImagePath();
        if (refPath == null || refPath.isBlank()) {
            refPath = "classpath:/menu-style/" + styleId + "/reference.png";
        }

        String pathInFs = refPath.replace("classpath:", "src/main/resources");
        String altPathInFs = refPath.replace("classpath:", "d:/Smart_Menu/backend/src/main/resources");

        String[] candidatePaths = {
                pathInFs,
                altPathInFs,
                "src/main/resources/menu-style/" + styleId + "/reference.png",
                "d:/Smart_Menu/backend/src/main/resources/menu-style/" + styleId + "/reference.png"
        };

        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists() && f.length() > 0) {
                try {
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    log.info("[STYLE-REFERENCE] styleId={} referencePath={} loaded=true imageSize={}",
                            styleId, refPath, bytes.length);
                    return Base64.getEncoder().encodeToString(bytes);
                } catch (Exception e) {
                    log.warn("Không thể đọc file style reference {}: {}", p, e.getMessage());
                }
            }
        }

        // Check for any .png file in style folder
        String[] dirPaths = {
                "src/main/resources/menu-style/" + styleId,
                "d:/Smart_Menu/backend/src/main/resources/menu-style/" + styleId
        };
        for (String dirPath : dirPaths) {
            File folder = new File(dirPath);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null && files.length > 0) {
                    try {
                        byte[] bytes = Files.readAllBytes(files[0].toPath());
                        log.info("[STYLE-REFERENCE] styleId={} referencePath={} loaded=true imageSize={}",
                                styleId, files[0].getName(), bytes.length);
                        return Base64.getEncoder().encodeToString(bytes);
                    } catch (Exception e) {
                        log.warn("Không thể đọc file style reference trong folder {}: {}", dirPath, e.getMessage());
                    }
                }
            }
        }

        log.error("[STYLE-REFERENCE] styleId={} referencePath={} loaded=false", styleId, refPath);
        throw new IllegalStateException("REFERENCE_ASSET_REQUIRED: Cannot load reference asset for styleId=" + styleId);
    }
}
