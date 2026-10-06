package com.smartmenu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmenu.dto.*;
import com.smartmenu.entity.*;
import com.smartmenu.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuGenerationService {

    private final MenuAnalysisSessionRepository sessionRepository;

    private final UploadedFileRepository uploadedFileRepository;

    private final ProcessedBusinessDataRepository processedBusinessDataRepository;

    private final BusinessMetricsRepository businessMetricsRepository;

    private final AIAnalysisResultRepository aiAnalysisResultRepository;

    private final GeneratedMenuRepository generatedMenuRepository;

    private final ExcelParserService excelParserService;

    private final BusinessAnalysisService businessAnalysisService;

    private final MenuImageService menuImageService;

    private final GeminiService geminiService;

    private final MenuBlueprintService menuBlueprintService;

    private final RestaurantRepository restaurantRepository;

    private final BusinessOptimizationService businessOptimizationService;

    private final FinalMenuPlanService finalMenuPlanService;

    private final FinalMenuPlanValidatorService finalMenuPlanValidatorService;

    private final PaymentService paymentService;

    private final ObjectMapper objectMapper;


    /**
     * Workflow chính SmartMenu
     *
     * 1. Lấy session
     * 2. Lấy file upload từ DB
     * 3. Backend đọc Excel
     * 4. Backend phân tích dữ liệu
     * 5. Gemini nhận dữ liệu đã xử lý
     * 6. Lưu kết quả
     */
    @Transactional
    public FullAnalysisResponseData analyzeFullWorkflow(
            Long sessionId) {

        log.info(
                "Start SmartMenu analysis session {}",
                sessionId);

        MenuAnalysisSession session = sessionRepository.findById(sessionId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Không tìm thấy session phân tích"));

        // Kiểm tra quyền sử dụng dịch vụ (01 lần Free hoặc đã thanh toán)
        Long userId = null;
        if (session.getRestaurantId() != null) {
            var restOpt = restaurantRepository.findById(session.getRestaurantId());
            if (restOpt.isPresent() && restOpt.get().getUserId() != null) {
                userId = restOpt.get().getUserId();
            }
        }
        if (userId == null) {
            throw new IllegalStateException("Phiên phân tích không có chủ sở hữu hợp lệ");
        }
        com.smartmenu.dto.AccessCheckResponseDto accessCheck = paymentService.checkAccess(userId);
        if (!accessCheck.isAllowed()) {
            throw new IllegalStateException("PAYMENT_REQUIRED: " + accessCheck.getMessage());
        }

        session.setStatus("PROCESSING");

        sessionRepository.save(session);

        /*
         * 1. Lấy file thật từ DB
         */

        List<UploadedFile> files = uploadedFileRepository
                .findBySessionId(sessionId);

        UploadedFile excelFile = files.stream()
                .filter(
                        f -> "BUSINESS_EXCEL"
                                .equalsIgnoreCase(
                                        f.getFileCategory()))
                .findFirst()
                .orElseThrow(
                        () -> new RuntimeException(
                                "Không tìm thấy file dữ liệu kinh doanh"));

        UploadedFile menuFile = files.stream()
                .filter(
                        f -> "OLD_MENU_IMAGE"
                                .equalsIgnoreCase(
                                        f.getFileCategory()))
                .findFirst()
                .orElseThrow(
                        () -> new RuntimeException(
                                "Không tìm thấy ảnh menu cũ"));

        /*
         * 2. Backend đọc Excel thật
         */

        Path excelPath = Paths.get(
                excelFile.getFilePath());

        if (!java.nio.file.Files.exists(excelPath)) {
            throw new RuntimeException("Không tìm thấy file Excel: " + excelPath);
        }

        List<ProductItemDto> products = excelParserService.parseExcel(
                excelPath,
                sessionId,
                excelFile.getId());

        if (products == null || products.isEmpty()) {

            throw new RuntimeException(
                    "Không có dữ liệu sản phẩm hợp lệ trong file Excel");

        }

        /*
         * 3. Backend tính toán nghiệp vụ
         */

        List<CategoryRevenueDto> categoryRevenue = new ArrayList<>();

        AnalysisReportDto analysisReport = businessAnalysisService.analyzeBusinessData(
                products,
                categoryRevenue);

        saveBusinessMetrics(
                sessionId,
                analysisReport);

        saveProcessedBusinessData(
                sessionId,
                products);

        /*
         * 4. Gemini Vision đọc bố cục menu
         */

        Path menuPath = Paths.get(
                menuFile.getFilePath());

        if (!java.nio.file.Files.exists(menuPath)) {
            throw new RuntimeException("Không tìm thấy ảnh menu cũ: " + menuPath);
        }

        menuImageService.validateMenuImage(
                menuPath);

        String menuBase64 = menuImageService.encodeFileToBase64(
                menuPath);

        String menuVisionResult = geminiService.analyzeMenuImage(
                menuBase64);

        /*
         * 5. AI phân tích Nhận định chi tiết & Lập Menu Restructure Plan
         */

        String detailedReasoning = geminiService.generateDetailedBusinessReasoning(
                analysisReport,
                products,
                categoryRevenue,
                menuVisionResult);

        /*
         * 5.1. Chạy BusinessOptimizationService (SOURCE OF TRUTH THUẦN BACKEND - TÁCH BIỆT HOÀN TOÀN KHỎI AI)
         */
        BusinessOptimizationResultDto optimizationResult = businessOptimizationService.optimizeBusinessData(
                sessionId,
                products,
                analysisReport,
                categoryRevenue
        );

        MenuRestructurePlanDto restructurePlan = mapOptimizationToRestructurePlan(optimizationResult, analysisReport);

        // Lưu AI Result & Optimization Strategy vào DB
        AIAnalysisResult result = AIAnalysisResult.builder()
                .sessionId(sessionId)
                .insight(detailedReasoning)
                .strategy(convertJson(restructurePlan))
                .build();
        aiAnalysisResultRepository.save(result);

        /*
         * 6. Trích xuất Brand Identity riêng cho SESSION NÀY & Tạo VisualMenuBlueprint (version 1)
         */
        String detectedBrandName = null;
        String logoUrl = null;
        String tagline = null;

        // 1. Ưu tiên từ thông tin Restaurant đã lưu cho session này
        if (session.getRestaurantId() != null) {
            Restaurant restaurant = restaurantRepository.findById(session.getRestaurantId()).orElse(null);
            if (restaurant != null && restaurant.getRestaurantName() != null 
                    && !restaurant.getRestaurantName().isBlank() 
                    && !"Nhà hàng Mặc định".equalsIgnoreCase(restaurant.getRestaurantName().trim())) {
                detectedBrandName = restaurant.getRestaurantName().trim();
            }
        }

        // 2. Nếu chưa có tên nhà hàng từ Restaurant, soi ảnh menu cũ của SESSION NÀY qua Gemini Vision
        if (detectedBrandName == null || detectedBrandName.isBlank()) {
            BrandIdentityDto brandIdentity = geminiService.extractBrandIdentityFromMenu(menuBase64);
            if (brandIdentity != null && brandIdentity.getBrandName() != null && !brandIdentity.getBrandName().isBlank()) {
                detectedBrandName = brandIdentity.getBrandName().trim();
                tagline = brandIdentity.getTagline();
                logoUrl = brandIdentity.getLogoUrl();

                // Cập nhật tên Restaurant cho session này để đồng nhất DB
                if (session.getRestaurantId() != null) {
                    restaurantRepository.findById(session.getRestaurantId()).ifPresent(r -> {
                        r.setRestaurantName(brandIdentity.getBrandName().trim());
                        restaurantRepository.save(r);
                    });
                }
            }
        }

        // 3. Nếu không xác định được cho session này: để rõ "Chưa xác định được thông tin thương hiệu"
        if (detectedBrandName == null || detectedBrandName.isBlank()) {
            detectedBrandName = "Chưa xác định được thông tin thương hiệu";
        }

        log.info("Session {} Brand Identity resolved: brandName='{}', tagline='{}'", sessionId, detectedBrandName, tagline);

        VisualMenuBlueprintDto blueprint = menuBlueprintService.buildBlueprint(
                sessionId,
                products,
                restructurePlan,
                optimizationResult,
                detectedBrandName,
                logoUrl,
                tagline
        );

        // BƯỚC 7: TẠO VÀ FREEZE FINAL MENU PLAN BẤT BIẾN CHO SESSION NÀY (MAP + FREEZE ONLY)
        FinalMenuPlanDto finalMenuPlan = finalMenuPlanService.createAndFreezeFinalMenuPlan(
                sessionId,
                products,
                analysisReport,
                optimizationResult,
                categoryRevenue
        );

        // PRE-AI CONTRACT VALIDATION LAYER (V4)
        finalMenuPlanValidatorService.validatePlan(finalMenuPlan);

        GeneratedMenuDataDto menuData = GeneratedMenuDataDto.builder()
                .sessionId(sessionId)
                .selectedStyleId("COFFEE_MODERN_01")
                .blueprintVersion(1)
                .blueprint(blueprint)
                .finalMenuPlan(finalMenuPlan)
                .build();

        GeneratedMenu generatedMenu = GeneratedMenu.builder()
                .sessionId(sessionId)
                .generationData(convertJson(menuData))
                .build();

        generatedMenuRepository.save(generatedMenu);

        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        // Ghi nhận lịch sử MENU_GENERATION và tiêu thụ quyền (cập nhật free_used hoặc consumed = true)
        paymentService.recordMenuGenerationAndConsume(userId, sessionId, "COFFEE_MODERN_01", generatedMenu.getMenuImageUrl());

        return FullAnalysisResponseData.builder()
                .sessionId(String.valueOf(sessionId))
                .analysis(analysisReport)
                .products(products)
                .categoryRevenue(categoryRevenue)
                .reasoning(detailedReasoning)
                .menuVisionIssues(menuVisionResult)
                .restructurePlan(restructurePlan)
                .optimizationResult(optimizationResult)
                .finalMenuPlan(finalMenuPlan)
                .build();

    }

    public FullAnalysisResponseData getAnalysisBySessionId(
            String sessionId) {

        Long id = Long.parseLong(
                sessionId);

        AIAnalysisResult aiResult = aiAnalysisResultRepository
                .findFirstBySessionIdOrderByIdDesc(id)
                .orElse(null);

        BusinessMetrics metrics = businessMetricsRepository
                .findBySessionId(id)
                .orElse(null);

        AnalysisReportDto report = null;

        if (metrics != null) {

            report = AnalysisReportDto.builder()

                    .totalRevenue(
                            metrics.getTotalRevenue()
                                    .doubleValue())

                    .totalItemsSold(
                            metrics.getTotalQuantity())

                    .estimatedProfit(
                            metrics.getEstimatedProfit()
                                    .doubleValue())

                    .optimizationScore(
                            metrics.getMenuScore())

                    .build();

        }

        String reasoning = null;
        MenuRestructurePlanDto restructurePlan = null;

        if (aiResult != null) {
            reasoning = aiResult.getInsight();
            try {
                if (aiResult.getStrategy() != null && aiResult.getStrategy().startsWith("{")) {
                    restructurePlan = objectMapper.readValue(
                            aiResult.getStrategy(),
                            MenuRestructurePlanDto.class
                    );
                }
            } catch (Exception e) {
                log.warn("Không thể parse restructurePlan JSON session cũ: {}", e.getMessage());
            }
        }

        return FullAnalysisResponseData.builder()
                .sessionId(sessionId)
                .analysis(report)
                .reasoning(reasoning)
                .restructurePlan(restructurePlan)
                .build();

    }

    private void saveAIResult3Tiers(
            Long sessionId,
            List<AiInsightDto> insights,
            List<AiStrategyDto> strategies,
            MenuRestructurePlanDto plan) {

        AIAnalysisResult result = AIAnalysisResult.builder()

                .sessionId(
                        sessionId)

                .insight(
                        convertJson(insights))

                .recommendation(
                        convertJson(strategies))

                .strategy(
                        convertJson(plan))

                .build();

        aiAnalysisResultRepository.save(
                result);

    }

    private void saveBusinessMetrics(
            Long sessionId,
            AnalysisReportDto report) {

        BusinessMetrics metrics = BusinessMetrics.builder()

                .sessionId(sessionId)

                .totalRevenue(
                        BigDecimal.valueOf(
                                report.getTotalRevenue()))

                .totalQuantity(
                        report.getTotalItemsSold())

                .estimatedProfit(
                        BigDecimal.valueOf(
                                report.getEstimatedProfit()))

                .menuScore(
                        report.getOptimizationScore())

                .build();

        businessMetricsRepository.save(
                metrics);

    }

    private void saveProcessedBusinessData(
            Long sessionId,
            List<ProductItemDto> products) {

        ProcessedBusinessData data = ProcessedBusinessData.builder()

                .sessionId(
                        sessionId)

                .analysisPayload(
                        convertJson(products))

                .build();

        processedBusinessDataRepository.save(
                data);

    }

    private void saveAIResult(
            Long sessionId,
            String insight,
            List<RecommendationDto> recommendations) {

        AIAnalysisResult result = AIAnalysisResult.builder()

                .sessionId(
                        sessionId)

                .insight(
                        insight)

                .recommendation(
                        convertJson(
                                recommendations))

                .strategy(
                        "Generated by Gemini based on processed business data")

                .build();

        aiAnalysisResultRepository.save(
                result);

    }

    private MenuRestructurePlanDto mapOptimizationToRestructurePlan(
            BusinessOptimizationResultDto optResult,
            AnalysisReportDto analysisReport
    ) {
        if (optResult == null) return new MenuRestructurePlanDto();

        List<ProductOptimizationDto> prods = optResult.getProducts() != null ? optResult.getProducts() : Collections.emptyList();

        List<String> coreDishes = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIncludeInFinalMenu()))
                .filter(p -> p.getBusinessTags() != null && (p.getBusinessTags().contains(BusinessTag.BEST_SELLER.name()) || p.getBusinessTags().contains(BusinessTag.HIGH_PROFIT.name()) || p.getBusinessTags().contains(BusinessTag.CORE_PRODUCT.name())))
                .map(ProductOptimizationDto::getProductName)
                .collect(Collectors.toList());

        List<String> heroDishes = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIncludeInFinalMenu()))
                .filter(p -> p.getBusinessTags() != null && p.getBusinessTags().contains(BusinessTag.BEST_SELLER.name()) && p.getBusinessTags().contains(BusinessTag.HIGH_PROFIT.name()))
                .map(ProductOptimizationDto::getProductName)
                .limit(3)
                .collect(Collectors.toList());

        if (heroDishes.isEmpty()) {
            heroDishes = prods.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIncludeInFinalMenu()))
                    .filter(p -> p.getBusinessTags() != null && p.getBusinessTags().contains(BusinessTag.HIGH_PROFIT.name()))
                    .map(ProductOptimizationDto::getProductName)
                    .limit(2)
                    .collect(Collectors.toList());
        }

        List<PriorityProductDto> priorityProducts = prods.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIncludeInFinalMenu()))
                .filter(p -> p.getBusinessTags() != null && (p.getBusinessTags().contains(BusinessTag.BEST_SELLER.name()) || p.getBusinessTags().contains(BusinessTag.HIGH_PROFIT.name())))
                .map(p -> PriorityProductDto.builder()
                        .productName(p.getProductName())
                        .category(p.getCategoryName())
                        .priorityType(p.getBusinessTags().contains(BusinessTag.HIGH_PROFIT.name()) ? "HIGH_PROFIT" : "BEST_SELLER")
                        .reason(p.getReason())
                        .build())
                .collect(Collectors.toList());

        List<String> catOrderNames = optResult.getCategoryOrder() != null
                ? optResult.getCategoryOrder().stream().map(CategoryOrderDto::getCategoryName).collect(Collectors.toList())
                : Collections.emptyList();

        List<String> reducedPriorityItems = prods.stream()
                .filter(p -> ProductDecision.REDUCE_PRIORITY.name().equals(p.getDecision()) || (p.getBusinessTags() != null && p.getBusinessTags().contains(BusinessTag.REDUCED_PRIORITY.name())))
                .map(ProductOptimizationDto::getProductName)
                .collect(Collectors.toList());

        List<String> comboPlacements = prods.stream()
                .filter(p -> ProductDecision.PRICE_DECREASE.name().equals(p.getDecision()))
                .map(p -> p.getProductName() + " (Giảm 10% + " + (p.getPromoGift() != null ? p.getPromoGift() : "Ưu đãi") + ")")
                .collect(Collectors.toList());
        if (comboPlacements.isEmpty() && optResult.getCombos() != null) {
            comboPlacements = optResult.getCombos().stream()
                    .map(c -> c.getComboName() + " (" + String.format("%,dđ", c.getComboPrice()) + ")")
                    .collect(Collectors.toList());
        }

        return MenuRestructurePlanDto.builder()
                .coreDishes(coreDishes)
                .heroDishes(heroDishes)
                .priorityProducts(priorityProducts)
                .categoryOrder(catOrderNames)
                .reducedPriorityItems(reducedPriorityItems)
                .comboPlacements(comboPlacements)
                .structuredCombos(optResult.getStructuredCombos())
                .priceRecommendations(optResult.getPriceRecommendations())
                .topZone("Vùng 1 (Đầu menu): Tên thương hiệu & Hero Dish (" + String.join(", ", heroDishes) + ")")
                .centerZone("Vùng 2 (Trung tâm): Nhóm món ưu tiên (" + String.join(" -> ", catOrderNames) + ")")
                .secondaryZone("Vùng 3 (Khu vực phụ): Món giảm ưu tiên & Combo ưu đãi")
                .finalLayoutSummary(String.format("Tái cấu trúc menu %d sản phẩm, %d nhóm món, %d đề xuất giá thử nghiệm.",
                        prods.size(), catOrderNames.size(), optResult.getPriceRecommendations() != null ? optResult.getPriceRecommendations().size() : 0))
                .build();
    }

    private String convertJson(
            Object object) {

        try {

            return objectMapper.writeValueAsString(
                    object);

        } catch (Exception e) {

            return "{}";

        }

    }

}
