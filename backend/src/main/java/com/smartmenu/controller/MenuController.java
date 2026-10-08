package com.smartmenu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmenu.dto.ApiResponse;
import com.smartmenu.dto.GeneratedMenuDataDto;
import com.smartmenu.entity.AIAnalysisResult;
import com.smartmenu.entity.GeneratedMenu;
import com.smartmenu.repository.AIAnalysisResultRepository;
import com.smartmenu.repository.GeneratedMenuRepository;
import com.smartmenu.service.SessionAccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartmenu.dto.VisualMenuBlueprintDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/smartcoffee/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final GeneratedMenuRepository generatedMenuRepository;
    private final AIAnalysisResultRepository aiAnalysisResultRepository;
    private final ObjectMapper objectMapper;
    private final com.smartmenu.service.PaymentService paymentService;
    private final SessionAccessService sessionAccessService;

    /**
     * Lấy menu mới được AI đề xuất
     */
    @GetMapping("/{sessionId}/generated")
    public ResponseEntity<ApiResponse<GeneratedMenu>> getGeneratedMenu(
            @PathVariable Long sessionId,
            HttpServletRequest request
    ) {
        sessionAccessService.requireSessionOwner(sessionId, request);
        GeneratedMenu menu = generatedMenuRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Chưa có menu mới được tạo"));

        return ResponseEntity.ok(ApiResponse.success(menu, "Lấy menu mới thành công"));
    }

    /**
     * Lấy chiến lược AI
     */
    @GetMapping("/{sessionId}/strategy")
    public ResponseEntity<ApiResponse<AIAnalysisResult>> getAIStrategy(
            @PathVariable Long sessionId,
            HttpServletRequest request
    ) {
        sessionAccessService.requireSessionOwner(sessionId, request);
        AIAnalysisResult result = aiAnalysisResultRepository.findFirstBySessionIdOrderByIdDesc(sessionId)
                .orElseThrow(() -> new RuntimeException("Chưa có kết quả AI"));

        return ResponseEntity.ok(ApiResponse.success(result, "Lấy chiến lược AI thành công"));
    }

    /**
     * Lấy dữ liệu VisualMenuBlueprint & SelectedStyle của Session
     */
    @GetMapping("/{sessionId}/blueprint")
    public ResponseEntity<ApiResponse<GeneratedMenuDataDto>> getMenuBlueprint(
            @PathVariable Long sessionId,
            HttpServletRequest request
    ) {
        sessionAccessService.requireSessionOwner(sessionId, request);
        GeneratedMenu menu = generatedMenuRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Chưa có menu mới được tạo cho session này"));

        try {
            GeneratedMenuDataDto menuData = objectMapper.readValue(
                    menu.getGenerationData(),
                    GeneratedMenuDataDto.class
            );

            // Strict Validation: Đảm bảo dữ liệu Blueprint hoàn toàn khớp với currentSessionId
            if (menuData.getSessionId() != null && !menuData.getSessionId().equals(sessionId)) {
                log.error("[SESSION DATA LEAK DETECTED] menuData.sessionId ({}) != currentSessionId ({})",
                        menuData.getSessionId(), sessionId);
                throw new IllegalStateException("Lỗi Validation: Dữ liệu Blueprint thuộc session khác, cấm truy cập!");
            }

            if (menuData.getBlueprint() != null && menuData.getBlueprint().getSessionId() != null
                    && !menuData.getBlueprint().getSessionId().equals(sessionId)) {
                log.error("[SESSION DATA LEAK DETECTED] blueprint.sessionId ({}) != currentSessionId ({})",
                        menuData.getBlueprint().getSessionId(), sessionId);
                throw new IllegalStateException("Lỗi Validation: VisualMenuBlueprint thuộc session khác!");
            }

            return ResponseEntity.ok(ApiResponse.success(menuData, "Lấy VisualMenuBlueprint thành công"));
        } catch (Exception e) {
            log.error("Lỗi parse GeneratedMenuDataDto: {}", e.getMessage());
            throw new RuntimeException("Dữ liệu Blueprint không hợp lệ", e);
        }
    }

    /**
     * Đổi Phong cách Menu (SelectedStyle)
     * TUYỆT ĐỐI KHÔNG GỌI LẠI AI / KHÔNG THAY ĐỔI BLUEPRINT
     */
    @PostMapping("/{sessionId}/style")
    public ResponseEntity<ApiResponse<GeneratedMenuDataDto>> updateMenuStyle(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        Long userId = sessionAccessService.requireSessionOwner(sessionId, httpRequest);
        String styleId = request.get("styleId");
        if (styleId == null || styleId.isBlank()) {
            throw new IllegalArgumentException("StyleId không được để trống");
        }

        // Bản Free được dùng phong cách Hiện Đại. COFFEE_MODERN_01 là mã
        // template đã lưu của chính phong cách này, không phải một style Plus.
        if (!isFreeModernStyle(styleId)) {
            var accessCheck = paymentService.checkAccess(userId);
            if (!"PAID".equalsIgnoreCase(accessCheck.getType())) {
                log.warn("[PLUS-REQUIRED] Style {} chỉ dành cho gói Plus. Session={}, UserId={}", styleId, sessionId, userId);
                return ResponseEntity.status(402).body(ApiResponse.error("PLUS_REQUIRED: Phong cách " + styleId + " chỉ dành cho gói Plus. Vui lòng mua gói Plus để trải nghiệm!"));
            }
        }

        GeneratedMenu menu = generatedMenuRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Chưa có menu được tạo cho session này"));

        try {
            GeneratedMenuDataDto menuData = objectMapper.readValue(
                    menu.getGenerationData(),
                    GeneratedMenuDataDto.class
            );

            // Chỉ cập nhật selectedStyleId, giữ nguyên 100% blueprint và version
            menuData.setSelectedStyleId(styleId.toUpperCase());

            menu.setGenerationData(objectMapper.writeValueAsString(menuData));
            generatedMenuRepository.save(menu);

            log.info("Cập nhật StyleId sang '{}' cho Session {} thành công (Blueprint Version: {})",
                    styleId, sessionId, menuData.getBlueprintVersion());

            return ResponseEntity.ok(ApiResponse.success(menuData, "Cập nhật phong cách menu thành công"));
        } catch (Exception e) {
            log.error("Lỗi cập nhật style cho session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Không thể cập nhật phong cách menu", e);
        }
    }

    private final com.smartmenu.service.FinalMenuPlanService finalMenuPlanService;
    private final com.smartmenu.service.AiMenuArtworkGeneratorService aiMenuArtworkGeneratorService;
    private final com.smartmenu.service.ArtworkContentValidatorService artworkContentValidatorService;

    /**
     * PRODUCTION ENDPOINT: POST /api/v1/smartcoffee/menu/{sessionId}/generate-ai-menu-artwork
     * Request body: { "styleId": "HIEN_DAI" }
     * Backend:
     * - Loads frozen FinalMenuPlanDto from session DB (Business Truth)
     * - Loads style reference images from resources/menu-style/{styleId}/
     * - AI Designer generates commercial F&B artwork PNG + render manifest
     * - Backend validates content (manifest matching vs FinalMenuPlanDto)
     * - Saves artwork image and updates DB
     * - Returns result with validation status
     */
    @PostMapping("/{sessionId}/generate-ai-menu-artwork")
    public ResponseEntity<ApiResponse<com.smartmenu.dto.AiMenuArtworkResultDto>> generateAiMenuArtwork(
            @PathVariable Long sessionId,
            @RequestBody(required = false) com.smartmenu.dto.GenerateAiArtworkRequestDto request,
            HttpServletRequest httpRequest
    ) {
        Long userId = sessionAccessService.requireSessionOwner(sessionId, httpRequest);
        String styleId = (request != null && request.getStyleId() != null && !request.getStyleId().isBlank()) 
                ? request.getStyleId() 
                : "HIEN_DAI";

        log.info("[AI-ARTWORK-API] Endpoint entered | sessionId={} | styleId={}", sessionId, styleId);

        // Bản Free được dùng phong cách Hiện Đại, gồm mã template đã lưu.
        if (!isFreeModernStyle(styleId)) {
            var accessCheck = paymentService.checkAccess(userId);
            if (!"PAID".equalsIgnoreCase(accessCheck.getType())) {
                log.warn("[PLUS-REQUIRED] Generate Artwork style {} yêu cầu gói Plus. Session={}, UserId={}", styleId, sessionId, userId);
                return ResponseEntity.status(402).body(ApiResponse.error("PLUS_REQUIRED: Bản dùng thử Free chỉ được tạo style Hiện Đại. Vui lòng mua gói Plus để trải nghiệm phong cách " + styleId));
            }
        }

        GeneratedMenu menu = generatedMenuRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalStateException("FINAL_MENU_PLAN_NOT_READY: Session " + sessionId + " chưa hoàn tất phân tích"));

        try {
            GeneratedMenuDataDto menuData = objectMapper.readValue(
                    menu.getGenerationData(),
                    GeneratedMenuDataDto.class
            );

            // 1. Load or freeze FinalMenuPlanDto (Business Truth Only)
            com.smartmenu.dto.FinalMenuPlanDto frozenPlan = menuData.getFinalMenuPlan();
            if (frozenPlan == null) {
                log.warn("[PLAN FALLBACK] Session {} chưa có FinalMenuPlan, tạo và đóng băng từ Blueprint hiện tại", sessionId);
                if (menuData.getBlueprint() == null || menuData.getBlueprint().getSections() == null) {
                    throw new IllegalStateException("FINAL_MENU_PLAN_NOT_READY: Không có dữ liệu để sinh FinalMenuPlan");
                }
                List<com.smartmenu.dto.ProductItemDto> products = extractProductsFromBlueprint(menuData.getBlueprint());
                frozenPlan = finalMenuPlanService.createAndFreezeFinalMenuPlan(sessionId, products, null, null, null);
                menuData.setFinalMenuPlan(frozenPlan);
                menu.setGenerationData(objectMapper.writeValueAsString(menuData));
                generatedMenuRepository.save(menu);
            }
            log.info("[BUSINESS TRUTH] Frozen plan loaded | planHash={}", frozenPlan.getPlanHash());

            // 2. Multimodal AI Designer generates Commercial Menu Artwork
            com.smartmenu.dto.AiMenuArtworkResultDto artworkResult = aiMenuArtworkGeneratorService.generateArtwork(frozenPlan, styleId);

            // 3. Content Validation: Level 1 Manifest Matching
            com.smartmenu.dto.ArtworkValidationResultDto validationResult = artworkContentValidatorService.validateArtworkContent(
                    frozenPlan, artworkResult.getRenderManifest(), artworkResult.getImageUrl()
            );
            artworkResult.setValidation(validationResult);

            if (!validationResult.isValid()) {
                log.error("[CONTENT VALIDATION REJECTED] Artwork sai lệch so với Business Truth: {}", validationResult.getDiscrepancies());
                artworkResult.setSuccess(false);
                artworkResult.setErrorMessage("REJECTED: Nội dung menu do AI sinh ra không khớp với dữ liệu kinh doanh FinalMenuPlanDto");
                return ResponseEntity.badRequest().body(ApiResponse.error("Validation Rejected: " + validationResult.getDiscrepancies()));
            }

            // 4. Save Image and Update DB
            menuData.setSelectedStyleId(styleId.toUpperCase());
            menu.setMenuImageUrl(artworkResult.getImageUrl());
            menu.setGenerationData(objectMapper.writeValueAsString(menuData));
            generatedMenuRepository.save(menu);

            log.info("[IMAGE SAVED] Session {} artwork saved | url={}", sessionId, artworkResult.getImageUrl());

            return ResponseEntity.ok(ApiResponse.success(artworkResult, "Sinh AI Menu Artwork và xác thực nội dung thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi sinh AI Menu Artwork cho session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo AI Menu Artwork: " + e.getMessage(), e);
        }
    }

    /**
     * Backward-compatible API: POST /{sessionId}/generate-final-menu
     */
    @PostMapping("/{sessionId}/generate-final-menu")
    public ResponseEntity<ApiResponse<com.smartmenu.dto.FinalMenuResponseDto>> generateFinalMenu(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        Long userId = sessionAccessService.requireSessionOwner(sessionId, httpRequest);
        String styleId = request.containsKey("styleId") ? String.valueOf(request.get("styleId")) : "HIEN_DAI";

        log.info("[LEGACY-API] generate-final-menu called | sessionId={} | styleId={}", sessionId, styleId);

        // Bản Free được dùng phong cách Hiện Đại, gồm mã template đã lưu.
        if (!isFreeModernStyle(styleId)) {
            var accessCheck = paymentService.checkAccess(userId);
            if (!"PAID".equalsIgnoreCase(accessCheck.getType())) {
                log.warn("[PLUS-REQUIRED] Generate Final Menu style {} yêu cầu gói Plus. Session={}, UserId={}", styleId, sessionId, userId);
                return ResponseEntity.status(402).body(ApiResponse.error("PLUS_REQUIRED: Bản dùng thử Free chỉ được tạo style Hiện Đại. Vui lòng mua gói Plus để trải nghiệm phong cách " + styleId));
            }
        }

        GeneratedMenu menu = generatedMenuRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalStateException("FINAL_MENU_PLAN_NOT_READY: Session " + sessionId + " chưa hoàn tất phân tích"));

        try {
            GeneratedMenuDataDto menuData = objectMapper.readValue(
                    menu.getGenerationData(),
                    GeneratedMenuDataDto.class
            );

            com.smartmenu.dto.FinalMenuPlanDto frozenPlan = menuData.getFinalMenuPlan();
            if (frozenPlan == null) {
                List<com.smartmenu.dto.ProductItemDto> products = extractProductsFromBlueprint(menuData.getBlueprint());
                frozenPlan = finalMenuPlanService.createAndFreezeFinalMenuPlan(sessionId, products, null, null, null);
                menuData.setFinalMenuPlan(frozenPlan);
                menu.setGenerationData(objectMapper.writeValueAsString(menuData));
                generatedMenuRepository.save(menu);
            }

            com.smartmenu.dto.AiMenuArtworkResultDto artworkResult = aiMenuArtworkGeneratorService.generateArtwork(frozenPlan, styleId);
            com.smartmenu.dto.ArtworkValidationResultDto validationResult = artworkContentValidatorService.validateArtworkContent(
                    frozenPlan, artworkResult.getRenderManifest(), artworkResult.getImageUrl()
            );

            menuData.setSelectedStyleId(styleId.toUpperCase());
            menu.setMenuImageUrl(artworkResult.getImageUrl());
            menu.setGenerationData(objectMapper.writeValueAsString(menuData));
            generatedMenuRepository.save(menu);

            int totalProducts = 0;
            if (frozenPlan.getCategories() != null) {
                for (var cat : frozenPlan.getCategories()) {
                    if (cat.getProducts() != null) {
                        for (var p : cat.getProducts()) {
                            if (Boolean.TRUE.equals(p.getIncludeInFinalMenu())) totalProducts++;
                        }
                    }
                }
            }

            boolean isLuxury = "SANG_TRONG".equalsIgnoreCase(styleId) || "LUXURY".equalsIgnoreCase(styleId);
            boolean isVintage = "TRUYEN_THONG".equalsIgnoreCase(styleId) || "VINTAGE".equalsIgnoreCase(styleId);
            int respWidth = isLuxury ? 1102 : (isVintage ? 682 : 800);
            int respHeight = isLuxury ? 762 : (isVintage ? 1024 : 1131);

            com.smartmenu.dto.FinalMenuResponseDto response = com.smartmenu.dto.FinalMenuResponseDto.builder()
                    .finalImageUrl(artworkResult.getImageUrl())
                    .templateId(styleId.toUpperCase())
                    .totalDishesCount(totalProducts)
                    .canvasWidth(respWidth)
                    .canvasHeight(respHeight)
                    .aspectRatio((double) respWidth / (double) respHeight)
                    .validated(validationResult.isValid())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, "Tạo ảnh Final Menu thành công"));
        } catch (Exception e) {
            log.error("Lỗi sinh Final Menu cho Session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo Final Menu", e);
        }
    }

    private boolean isFreeModernStyle(String styleId) {
        if (styleId == null) {
            return true;
        }

        String normalizedStyle = styleId.trim().toUpperCase(java.util.Locale.ROOT);
        return "HIEN_DAI".equals(normalizedStyle)
                || "MODERN".equals(normalizedStyle)
                || "COFFEE_MODERN_01".equals(normalizedStyle);
    }

    private List<com.smartmenu.dto.ProductItemDto> extractProductsFromBlueprint(VisualMenuBlueprintDto blueprint) {
        List<com.smartmenu.dto.ProductItemDto> list = new ArrayList<>();
        if (blueprint.getSections() == null) return list;
        int idCount = 1;
        for (var sec : blueprint.getSections()) {
            if (sec.getDishes() == null) continue;
            for (var dish : sec.getDishes()) {
                list.add(com.smartmenu.dto.ProductItemDto.builder()
                        .id("prod_" + idCount++)
                        .name(dish.getName())
                        .category(sec.getCategoryName())
                        .originalPrice(dish.getOriginalPrice())
                        .salesQuantity(dish.getSalesQuantity())
                        .profitMargin(dish.getProfitMargin())
                        .bcgCategory(dish.getBcgCategory())
                        .build());
            }
        }
        return list;
    }
}
