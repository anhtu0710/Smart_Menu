package com.smartmenu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmenu.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessOptimizationServiceTest {

    private BusinessAnalysisService businessAnalysisService;
    private BusinessOptimizationService businessOptimizationService;
    private FinalMenuPlanService finalMenuPlanService;
    private AiFinalMenuDesignerService aiFinalMenuDesignerService;
    private FinalMenuContentBinderService finalMenuContentBinderService;

    private static final String SAMPLE_ARTWORK_BASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    public static org.springframework.web.client.RestTemplate createMockAiRestTemplate(String sampleDataUri) {
        org.springframework.web.client.RestTemplate mockRestTemplate = org.mockito.Mockito.mock(org.springframework.web.client.RestTemplate.class);
        String responseJson = String.format("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "%s"
                          }
                        ]
                      }
                    }
                  ]
                }
                """, sampleDataUri);
        org.mockito.Mockito.when(mockRestTemplate.postForEntity(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(String.class)
        )).thenReturn(new org.springframework.http.ResponseEntity<>(responseJson, org.springframework.http.HttpStatus.OK));
        return mockRestTemplate;
    }

    @BeforeEach
    void setUp() {
        businessAnalysisService = new BusinessAnalysisService();
        businessOptimizationService = new BusinessOptimizationService();
        ObjectMapper objectMapper = new ObjectMapper();
        finalMenuPlanService = new FinalMenuPlanService(objectMapper);
        MenuStyleConfigRegistry registry = new MenuStyleConfigRegistry();
        org.springframework.web.client.RestTemplate mockAi = createMockAiRestTemplate(SAMPLE_ARTWORK_BASE64);
        AiMenuArtworkGeneratorService artworkGen = new AiMenuArtworkGeneratorService(mockAi, objectMapper, registry);
        aiFinalMenuDesignerService = new AiFinalMenuDesignerService(objectMapper, registry, artworkGen, new ArtworkContentValidatorService());
        finalMenuContentBinderService = new FinalMenuContentBinderService();
    }

    @Test
    @DisplayName("Test Refined Reduced Priority & Core Product Classification Rules (4 Cases Spec)")
    void testRefinedProductClassificationRules() {
        List<ProductItemDto> products = new ArrayList<>();

        // Case 4: Cà phê sữa đá (Quantity=360, Margin=65.7%) -> BEST_SELLER + HIGH_PROFIT -> PRICE_INCREASE
        products.add(ProductItemDto.builder()
                .id("C001").name("Cà phê sữa đá").category("Cà phê")
                .originalPrice(35000.0).costPrice(12000.0).salesQuantity(360)
                .build());

        // Case 1: Cappuccino (Quantity=110, Margin=59.6%) -> CORE_PRODUCT -> KEEP
        products.add(ProductItemDto.builder()
                .id("C002").name("Cappuccino").category("Cà phê")
                .originalPrice(45000.0).costPrice(18180.0).salesQuantity(110)
                .build());

        // Case 3: Matcha đá xay (Quantity=140, Margin=58.6%) -> CORE_PRODUCT -> KEEP
        products.add(ProductItemDto.builder()
                .id("C003").name("Matcha đá xay").category("Đá xay")
                .originalPrice(49000.0).costPrice(20286.0).salesQuantity(140)
                .build());

        // Case 2: Espresso tonic (Quantity=20, Margin=18.75%) -> REDUCED_PRIORITY -> REMOVE
        products.add(ProductItemDto.builder()
                .id("C004").name("Espresso tonic").category("Cà phê")
                .originalPrice(40000.0).costPrice(32500.0).salesQuantity(20)
                .build());

        List<CategoryRevenueDto> categoryRevenues = new ArrayList<>();

        // 1. RUN BUSINESS ANALYSIS
        AnalysisReportDto analysisReport = businessAnalysisService.analyzeBusinessData(products, categoryRevenues);
        assertNotNull(analysisReport);

        // 2. RUN BUSINESS OPTIMIZATION
        BusinessOptimizationResultDto optResult = businessOptimizationService.optimizeBusinessData(
                202L, products, analysisReport, categoryRevenues
        );

        assertNotNull(optResult);
        assertEquals(4, optResult.getProducts().size());

        // VERIFY CASE 4: Cà phê sữa đá -> BEST_SELLER + HIGH_PROFIT -> PRICE_INCREASE
        ProductOptimizationDto caPheSua = optResult.getProducts().stream()
                .filter(p -> "Cà phê sữa đá".equalsIgnoreCase(p.getProductName()))
                .findFirst().orElseThrow();
        assertTrue(caPheSua.getBusinessTags().contains("BEST_SELLER"));
        assertTrue(caPheSua.getBusinessTags().contains("HIGH_PROFIT"));
        assertEquals("PRICE_INCREASE", caPheSua.getDecision());

        // VERIFY CASE 1: Cappuccino -> HIGH_PROFIT or CORE_PRODUCT -> KEEP (Không bị đánh nhầm sang REDUCED_PRIORITY)
        ProductOptimizationDto cappuccino = optResult.getProducts().stream()
                .filter(p -> "Cappuccino".equalsIgnoreCase(p.getProductName()))
                .findFirst().orElseThrow();
        assertTrue(cappuccino.getBusinessTags().contains("HIGH_PROFIT") || cappuccino.getBusinessTags().contains("CORE_PRODUCT"));
        assertFalse(cappuccino.getBusinessTags().contains("REDUCED_PRIORITY"));
        assertEquals("KEEP", cappuccino.getDecision());

        // VERIFY CASE 3: Matcha đá xay -> CORE_PRODUCT / HIGH_PROFIT -> KEEP
        ProductOptimizationDto matcha = optResult.getProducts().stream()
                .filter(p -> "Matcha đá xay".equalsIgnoreCase(p.getProductName()))
                .findFirst().orElseThrow();
        assertTrue(matcha.getBusinessTags().contains("HIGH_PROFIT") || matcha.getBusinessTags().contains("CORE_PRODUCT"));
        assertFalse(matcha.getBusinessTags().contains("REDUCED_PRIORITY"));
        assertEquals("KEEP", matcha.getDecision());

        // VERIFY CASE 2: Espresso tonic -> REDUCED_PRIORITY -> REMOVE
        ProductOptimizationDto espressoTonic = optResult.getProducts().stream()
                .filter(p -> "Espresso tonic".equalsIgnoreCase(p.getProductName()))
                .findFirst().orElseThrow();
        assertTrue(espressoTonic.getBusinessTags().contains("REDUCED_PRIORITY"));
        assertEquals("REMOVE", espressoTonic.getDecision());
        assertFalse(espressoTonic.getIncludeInFinalMenu());
    }

    @Test
    @DisplayName("AI Designer Render Test: Dataset Nhật Bản Session 999 (COFFEE_MODERN_01)")
    void testAiDesignerRenderSession999(@TempDir Path tempDir) throws Exception {
        // DATASET HOÀN TOÀN MỚI: THỰC ĐƠN QUÁN ĂN NHẬT BẢN
        List<ProductItemDto> products = new ArrayList<>();

        products.add(ProductItemDto.builder()
                .id("JP101").name("Sashimi Cá Hồi").category("Sushi & Sashimi")
                .originalPrice(320000.0).costPrice(100000.0).salesQuantity(250)
                .build());

        products.add(ProductItemDto.builder()
                .id("JP102").name("Sushi Lươn Nhật").category("Sushi & Sashimi")
                .originalPrice(180000.0).costPrice(50000.0).salesQuantity(300)
                .build());

        products.add(ProductItemDto.builder()
                .id("JP103").name("Tempura Tôm").category("Sushi & Sashimi")
                .originalPrice(150000.0).costPrice(45000.0).salesQuantity(90)
                .build());

        products.add(ProductItemDto.builder()
                .id("JP104").name("Ramen Tonkotsu").category("Mì & Lẩu")
                .originalPrice(120000.0).costPrice(55000.0).salesQuantity(210)
                .build());

        products.add(ProductItemDto.builder()
                .id("JP105").name("Mì Udon Bò Kobe").category("Mì & Lẩu")
                .originalPrice(160000.0).costPrice(60000.0).salesQuantity(200)
                .build());

        products.add(ProductItemDto.builder()
                .id("JP106").name("Bánh Xèo Okonomiyaki").category("Món phụ & Đồ uống")
                .originalPrice(99000.0).costPrice(50000.0).salesQuantity(15)
                .build());

        products.add(ProductItemDto.builder()
                .id("JP107").name("Rượu Sake Thường").category("Món phụ & Đồ uống")
                .originalPrice(250000.0).costPrice(230000.0).salesQuantity(3)
                .build());

        List<CategoryRevenueDto> categoryRevenues = new ArrayList<>();

        // 1. PIPELINE STEP 1: BUSINESS ANALYSIS
        AnalysisReportDto analysisReport = businessAnalysisService.analyzeBusinessData(products, categoryRevenues);

        // 2. PIPELINE STEP 2: BUSINESS OPTIMIZATION
        BusinessOptimizationResultDto optResult = businessOptimizationService.optimizeBusinessData(
                999L, products, analysisReport, categoryRevenues
        );

        // 3. PIPELINE STEP 3: FINAL MENU PLAN FREEZE (SESSION 999)
        FinalMenuPlanDto finalPlan = finalMenuPlanService.createAndFreezeFinalMenuPlan(
                999L, products, analysisReport, optResult, categoryRevenues
        );

        // 4. PIPELINE STEP 4: AI DESIGNER GENERATE WITH STYLE 'COFFEE_MODERN_01'
        String rawSvgOutput = aiFinalMenuDesignerService.generateStructuredSvgDesign(finalPlan, "COFFEE_MODERN_01", null);

        // 5. PIPELINE STEP 5: CONTENT BINDER TIÊM DỮ LIỆU ĐÓNG BĂNG TỪ FINAL MENU PLAN VÀO DOM
        String boundSvg = finalMenuContentBinderService.bindLockedContent(rawSvgOutput, finalPlan);

        // 6. VALIDATE CONTRACTS & BUSINESS TRUTH INTEGRITY:
        List<ProductPlanDto> normalProds = finalPlan.getCategories().stream()
                .flatMap(c -> c.getProducts().stream())
                .filter(p -> Boolean.TRUE.equals(p.getIncludeInFinalMenu()))
                .toList();
        assertEquals(6, normalProds.size());

        // Backend confirms Business Truth preservation
        assertTrue(rawSvgOutput.contains("reference-applied=\"true\""), "Artwork phải áp dụng reference image");
        assertTrue(rawSvgOutput.contains("style-id=\"COFFEE_MODERN_01\""), "Artwork phải áp dụng style COFFEE_MODERN_01");
        assertTrue(rawSvgOutput.contains("<image href=\"data:image/png;base64,"), "Artwork SVG container phải nhúng đúng tác phẩm ảnh AI PNG");

        // Verify excluded product is removed
        List<String> excludedIds = finalPlan.getExcludedProductIds();
        assertNotNull(excludedIds);
        assertTrue(excludedIds.contains("JP107"), "Món giảm ưu tiên JP107 phải bị loại khỏi danh sách");

        // 7. VERIFY AI ARTWORK & CONTENT VALIDATION
        ArtworkContentValidatorService validator = new ArtworkContentValidatorService();
        AiArtworkManifestDto manifest = aiFinalMenuDesignerService.getAiMenuArtworkGeneratorService() != null
                ? aiFinalMenuDesignerService.getAiMenuArtworkGeneratorService().generateArtwork(finalPlan, "COFFEE_MODERN_01").getRenderManifest()
                : null;

        ArtworkValidationResultDto valResult = validator.validateArtworkContent(finalPlan, manifest, "/generated/menu_999_hien_dai.png");
        assertTrue(valResult.isValid(), "Content Validation phải PASSED khi tất cả 6 sản phẩm khớp Business Truth");
        assertEquals(6, valResult.getMatchedItemsCount());
        assertEquals("PASSED", valResult.getStatus());

        Files.writeString(tempDir.resolve("final_menu_session_999.svg"), boundSvg);
    }

    @Test
    @DisplayName("Audit Test: Coffee Dataset Reduced Priority Rules (Strict 3-AND Formula)")
    void testCoffeeDatasetReducedPriorityAudit() {
        List<ProductItemDto> products = new ArrayList<>();

        // 1. Cà phê sữa đá: Best Seller + High Profit
        products.add(ProductItemDto.builder()
                .id("CF01").name("Cà phê sữa đá").category("Cà phê")
                .originalPrice(35000.0).costPrice(12000.0).salesQuantity(360)
                .build());

        // 2. Matcha đá xay: quantity >= categoryMedianQuantity -> KHÔNG REDUCED_PRIORITY
        products.add(ProductItemDto.builder()
                .id("CF02").name("Matcha đá xay").category("Đá xay")
                .originalPrice(49000.0).costPrice(20286.0).salesQuantity(140)
                .build());

        // 3. Cappuccino: quantity & margin ở mức trung bình -> CORE_PRODUCT
        products.add(ProductItemDto.builder()
                .id("CF03").name("Cappuccino").category("Cà phê")
                .originalPrice(45000.0).costPrice(18180.0).salesQuantity(110)
                .build());

        // 4. Americano: quantity thấp nhưng margin cao (66.7% >= median 59.6%) -> KHÔNG REDUCED_PRIORITY (CORE_PRODUCT)
        products.add(ProductItemDto.builder()
                .id("CF04").name("Americano").category("Cà phê")
                .originalPrice(30000.0).costPrice(10000.0).salesQuantity(30)
                .build());

        // 5. Latte caramel: quantity < median AND revenue < median AND margin < median -> REDUCED_PRIORITY
        products.add(ProductItemDto.builder()
                .id("CF05").name("Latte caramel").category("Cà phê")
                .originalPrice(35000.0).costPrice(30000.0).salesQuantity(24)
                .build());

        // 6. Espresso tonic: quantity < median AND revenue < median AND margin < median -> REDUCED_PRIORITY
        products.add(ProductItemDto.builder()
                .id("CF06").name("Espresso tonic").category("Cà phê")
                .originalPrice(40000.0).costPrice(32500.0).salesQuantity(20)
                .build());

        List<CategoryRevenueDto> categoryRevenues = new ArrayList<>();

        // 1. RUN ANALYSIS
        AnalysisReportDto analysisReport = businessAnalysisService.analyzeBusinessData(products, categoryRevenues);

        // 2. RUN OPTIMIZATION
        BusinessOptimizationResultDto optResult = businessOptimizationService.optimizeBusinessData(
                303L, products, analysisReport, categoryRevenues
        );

        assertNotNull(optResult);

        // VERIFY AMERICANO: margin 66.7% >= category median margin -> KHÔNG bị REDUCED_PRIORITY
        ProductOptimizationDto americano = optResult.getProducts().stream()
                .filter(p -> "Americano".equalsIgnoreCase(p.getProductName()))
                .findFirst().orElseThrow();
        assertFalse(americano.getBusinessTags().contains("REDUCED_PRIORITY"), "Americano có margin >= median không được đánh REDUCED_PRIORITY");
        assertEquals("KEEP", americano.getDecision());

        // VERIFY MATCHA ĐÁ XAY: quantity 140 >= category median quantity -> KHÔNG bị REDUCED_PRIORITY
        ProductOptimizationDto matcha = optResult.getProducts().stream()
                .filter(p -> "Matcha đá xay".equalsIgnoreCase(p.getProductName()))
                .findFirst().orElseThrow();
        assertFalse(matcha.getBusinessTags().contains("REDUCED_PRIORITY"), "Matcha đá xay có quantity >= median không được đánh REDUCED_PRIORITY");
        assertTrue(matcha.getBusinessTags().contains("HIGH_PROFIT") || matcha.getBusinessTags().contains("CORE_PRODUCT"));
        assertEquals("KEEP", matcha.getDecision());

        // VERIFY LATTE CARAMEL: Qty < median AND Rev < median AND Margin < median -> REDUCED_PRIORITY
        ProductOptimizationDto latteCaramel = optResult.getProducts().stream()
                .filter(p -> "Latte caramel".equalsIgnoreCase(p.getProductName()))
                .findFirst().orElseThrow();
        assertTrue(latteCaramel.getBusinessTags().contains("REDUCED_PRIORITY"), "Latte caramel thỏa 3 điều kiện phải bị đánh REDUCED_PRIORITY");

        // VERIFY CAPPUCCINO: CORE_PRODUCT / HIGH_PROFIT, KEEP
        ProductOptimizationDto cappuccino = optResult.getProducts().stream()
                .filter(p -> "Cappuccino".equalsIgnoreCase(p.getProductName()))
                .findFirst().orElseThrow();
        assertTrue(cappuccino.getBusinessTags().contains("HIGH_PROFIT") || cappuccino.getBusinessTags().contains("CORE_PRODUCT"));
        assertTrue("PRICE_INCREASE".equals(cappuccino.getDecision()) || "KEEP".equals(cappuccino.getDecision()));
    }

    @Test
    @DisplayName("Dynamic Dataset Test 1: Coffee Category Benchmark & Numerical Tagging")
    void testDataset1Coffee() {
        List<ProductItemDto> products = new ArrayList<>();
        products.add(ProductItemDto.builder().id("C1").name("Cà phê sữa đá").category("Cà phê").originalPrice(35000.0).costPrice(12000.0).salesQuantity(360).build());
        products.add(ProductItemDto.builder().id("C2").name("Cappuccino").category("Cà phê").originalPrice(45000.0).costPrice(18180.0).salesQuantity(110).build());
        products.add(ProductItemDto.builder().id("C3").name("Americano").category("Cà phê").originalPrice(30000.0).costPrice(10000.0).salesQuantity(30).build());
        products.add(ProductItemDto.builder().id("C4").name("Latte caramel").category("Cà phê").originalPrice(35000.0).costPrice(30000.0).salesQuantity(24).build());
        products.add(ProductItemDto.builder().id("C5").name("Espresso tonic").category("Cà phê").originalPrice(40000.0).costPrice(32500.0).salesQuantity(20).build());

        List<CategoryRevenueDto> categoryRevenues = new ArrayList<>();
        AnalysisReportDto report = businessAnalysisService.analyzeBusinessData(products, categoryRevenues);
        BusinessOptimizationResultDto opt = businessOptimizationService.optimizeBusinessData(101L, products, report, categoryRevenues);

        assertEquals(5, opt.getProducts().size());

        ProductOptimizationDto caPheSua = opt.getProducts().stream().filter(p -> "Cà phê sữa đá".equals(p.getProductName())).findFirst().orElseThrow();
        assertTrue(caPheSua.getBusinessTags().contains("BEST_SELLER"));
        assertEquals("PRICE_INCREASE", caPheSua.getDecision());

        ProductOptimizationDto latte = opt.getProducts().stream().filter(p -> "Latte caramel".equals(p.getProductName())).findFirst().orElseThrow();
        assertTrue(latte.getBusinessTags().contains("REDUCED_PRIORITY"));
        assertEquals("REMOVE", latte.getDecision());
    }

    @Test
    @DisplayName("Dynamic Dataset Test 2: Restaurant (Quán Ăn Việt Nam) Category Benchmark")
    void testDataset2Restaurant() {
        List<ProductItemDto> products = new ArrayList<>();
        products.add(ProductItemDto.builder().id("R1").name("Cơm chiên hải sản").category("Món chính").originalPrice(85000.0).costPrice(30000.0).salesQuantity(500).build());
        products.add(ProductItemDto.builder().id("R2").name("Bò lúc lắc").category("Món chính").originalPrice(120000.0).costPrice(50000.0).salesQuantity(350).build());
        products.add(ProductItemDto.builder().id("R3").name("Gỏi ngó sen tôm thịt").category("Món chính").originalPrice(95000.0).costPrice(40000.0).salesQuantity(200).build());
        products.add(ProductItemDto.builder().id("R4").name("Mực chiên giòn").category("Món chính").originalPrice(110000.0).costPrice(70000.0).salesQuantity(80).build());
        products.add(ProductItemDto.builder().id("R5").name("Lẩu Thái hải sản").category("Món chính").originalPrice(250000.0).costPrice(220000.0).salesQuantity(15).build());

        List<CategoryRevenueDto> categoryRevenues = new ArrayList<>();
        AnalysisReportDto report = businessAnalysisService.analyzeBusinessData(products, categoryRevenues);
        BusinessOptimizationResultDto opt = businessOptimizationService.optimizeBusinessData(102L, products, report, categoryRevenues);

        assertEquals(5, opt.getProducts().size());

        ProductOptimizationDto comChien = opt.getProducts().stream().filter(p -> "Cơm chiên hải sản".equals(p.getProductName())).findFirst().orElseThrow();
        assertTrue(comChien.getBusinessTags().contains("BEST_SELLER"));

        ProductOptimizationDto lauThai = opt.getProducts().stream().filter(p -> "Lẩu Thái hải sản".equals(p.getProductName())).findFirst().orElseThrow();
        assertTrue(lauThai.getBusinessTags().contains("REDUCED_PRIORITY"));
        assertEquals("REMOVE", lauThai.getDecision());
    }

    @Test
    @DisplayName("Dynamic Dataset Test 3: Japanese Food Category Benchmark")
    void testDataset3JapaneseFood() {
        List<ProductItemDto> products = new ArrayList<>();
        products.add(ProductItemDto.builder().id("J1").name("Sushi Lươn Nhật").category("Sushi & Sashimi").originalPrice(180000.0).costPrice(50000.0).salesQuantity(300).build());
        products.add(ProductItemDto.builder().id("J2").name("Sashimi Cá Hồi").category("Sushi & Sashimi").originalPrice(320000.0).costPrice(100000.0).salesQuantity(250).build());
        products.add(ProductItemDto.builder().id("J3").name("Tempura Tôm").category("Sushi & Sashimi").originalPrice(150000.0).costPrice(45000.0).salesQuantity(90).build());
        products.add(ProductItemDto.builder().id("J4").name("Bánh Xèo Okonomiyaki").category("Món phụ & Đồ uống").originalPrice(99000.0).costPrice(50000.0).salesQuantity(15).build());
        products.add(ProductItemDto.builder().id("J5").name("Rượu Sake Thường").category("Món phụ & Đồ uống").originalPrice(250000.0).costPrice(230000.0).salesQuantity(3).build());

        List<CategoryRevenueDto> categoryRevenues = new ArrayList<>();
        AnalysisReportDto report = businessAnalysisService.analyzeBusinessData(products, categoryRevenues);
        BusinessOptimizationResultDto opt = businessOptimizationService.optimizeBusinessData(103L, products, report, categoryRevenues);

        assertEquals(5, opt.getProducts().size());

        ProductOptimizationDto sushi = opt.getProducts().stream().filter(p -> "Sushi Lươn Nhật".equals(p.getProductName())).findFirst().orElseThrow();
        assertTrue(sushi.getBusinessTags().contains("BEST_SELLER"));

        ProductOptimizationDto sake = opt.getProducts().stream().filter(p -> "Rượu Sake Thường".equals(p.getProductName())).findFirst().orElseThrow();
        assertTrue(sake.getBusinessTags().contains("REDUCED_PRIORITY"));
        assertEquals("REMOVE", sake.getDecision());
    }

    @Test
    @DisplayName("Dynamic Dataset Test 4: Milk Tea (Trà sữa) Category Benchmark")
    void testDataset4MilkTea() {
        List<ProductItemDto> products = new ArrayList<>();
        products.add(ProductItemDto.builder().id("M1").name("Trà sữa Trân châu Đường đen").category("Trà sữa").originalPrice(55000.0).costPrice(15000.0).salesQuantity(800).build());
        products.add(ProductItemDto.builder().id("M2").name("Trà ô long Kem cheese").category("Trà sữa").originalPrice(60000.0).costPrice(20000.0).salesQuantity(650).build());
        products.add(ProductItemDto.builder().id("M3").name("Trà hoa cúc Mật ong").category("Trà sữa").originalPrice(45000.0).costPrice(15000.0).salesQuantity(300).build());
        products.add(ProductItemDto.builder().id("M4").name("Trà xoài Macchiato").category("Trà sữa").originalPrice(50000.0).costPrice(35000.0).salesQuantity(100).build());
        products.add(ProductItemDto.builder().id("M5").name("Trà vải Thạch dừa").category("Trà sữa").originalPrice(40000.0).costPrice(32000.0).salesQuantity(50).build());

        List<CategoryRevenueDto> categoryRevenues = new ArrayList<>();
        AnalysisReportDto report = businessAnalysisService.analyzeBusinessData(products, categoryRevenues);
        BusinessOptimizationResultDto opt = businessOptimizationService.optimizeBusinessData(104L, products, report, categoryRevenues);

        assertEquals(5, opt.getProducts().size());

        ProductOptimizationDto traSua = opt.getProducts().stream().filter(p -> "Trà sữa Trân châu Đường đen".equals(p.getProductName())).findFirst().orElseThrow();
        assertTrue(traSua.getBusinessTags().contains("BEST_SELLER"));
        assertTrue(traSua.getBusinessTags().contains("HIGH_PROFIT"));
        assertEquals("PRICE_INCREASE", traSua.getDecision());

        ProductOptimizationDto traVai = opt.getProducts().stream().filter(p -> "Trà vải Thạch dừa".equals(p.getProductName())).findFirst().orElseThrow();
        assertTrue(traVai.getBusinessTags().contains("REDUCED_PRIORITY"));
        assertEquals("REMOVE", traVai.getDecision());
    }

    @Test
    @DisplayName("Test 1 - AI Success: AI Artwork generation produces valid image and manifest matching business plan")
    void testAiArtworkGenerationSuccess() {
        org.springframework.web.client.RestTemplate mockAi = createMockAiRestTemplate(SAMPLE_ARTWORK_BASE64);
        AiMenuArtworkGeneratorService generator = new AiMenuArtworkGeneratorService(mockAi, new ObjectMapper(), new MenuStyleConfigRegistry());
        ArtworkContentValidatorService validator = new ArtworkContentValidatorService();

        FinalMenuPlanDto plan = new FinalMenuPlanDto();
        plan.setSessionId("8001");
        CategoryPlanDto cat = new CategoryPlanDto();
        cat.setCategoryName("Signature Drinks");
        cat.setProducts(List.of(
                ProductPlanDto.builder().productId("SIG01").productName("Cold Brew Cam").finalDisplayPrice(55000.0).displayGroup("HERO").includeInFinalMenu(true).build(),
                ProductPlanDto.builder().productId("SIG02").productName("Espresso").finalDisplayPrice(30000.0).displayGroup("CORE").includeInFinalMenu(true).build()
        ));
        plan.setCategories(List.of(cat));

        AiMenuArtworkResultDto result = generator.generateArtwork(plan, "HIEN_DAI");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getImageUrl(), "imageUrl không được null khi AI sinh thành công");
        assertTrue(result.getImageUrl().startsWith("data:image/png;base64,"));

        // Manifest matches business plan exactly
        assertNotNull(result.getRenderManifest());
        assertEquals(1, result.getRenderManifest().getCategories().size());
        assertEquals(2, result.getRenderManifest().getCategories().get(0).getProducts().size());

        ArtworkValidationResultDto valResult = validator.validateArtworkContent(plan, result.getRenderManifest(), result.getImageUrl());
        assertTrue(valResult.isValid(), "Validation phải PASSED");
        assertEquals("PASSED", valResult.getStatus());
        assertEquals(2, valResult.getMatchedItemsCount());
    }

    @Test
    @DisplayName("Test 2 - AI Failure: When AI generation fails, throw AI_ARTWORK_GENERATION_FAILED immediately without fake menu")
    void testAiArtworkGenerationFailureThrowsException() {
        // RestTemplate is null or throwing exception to simulate AI failure
        AiMenuArtworkGeneratorService generator = new AiMenuArtworkGeneratorService(null, new ObjectMapper(), new MenuStyleConfigRegistry());

        FinalMenuPlanDto plan = new FinalMenuPlanDto();
        plan.setSessionId("8002");
        CategoryPlanDto cat = new CategoryPlanDto();
        cat.setCategoryName("Beverages");
        cat.setProducts(List.of(
                ProductPlanDto.builder().productId("B01").productName("Americano").finalDisplayPrice(35000.0).displayGroup("CORE").includeInFinalMenu(true).build()
        ));
        plan.setCategories(List.of(cat));

        // Must throw IllegalStateException with message containing AI_ARTWORK_GENERATION_FAILED
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            generator.generateArtwork(plan, "HIEN_DAI");
        });

        assertEquals("AI_ARTWORK_GENERATION_FAILED", ex.getMessage(), 
                "Hệ thống bắt buộc phải ném AI_ARTWORK_GENERATION_FAILED, không được âm thầm hạ cấp về demo renderer");
    }

    @Test
    @DisplayName("Test 3 - Dynamic Dataset: Style TRE_TRUNG with Japanese/Italian menu (Pizza Seafood 189000 & Ramen Tonkotsu 120000)")
    void testDynamicDatasetTreTrungWithPizzaAndRamen() {
        org.springframework.web.client.RestTemplate mockAi = createMockAiRestTemplate(SAMPLE_ARTWORK_BASE64);
        AiMenuArtworkGeneratorService generator = new AiMenuArtworkGeneratorService(mockAi, new ObjectMapper(), new MenuStyleConfigRegistry());
        ArtworkContentValidatorService validator = new ArtworkContentValidatorService();

        FinalMenuPlanDto plan = new FinalMenuPlanDto();
        plan.setSessionId("8003");
        CategoryPlanDto mainCourse = new CategoryPlanDto();
        mainCourse.setCategoryName("Main Dishes");
        mainCourse.setProducts(List.of(
                ProductPlanDto.builder().productId("IT01").productName("Pizza Seafood").finalDisplayPrice(189000.0).displayGroup("HERO").includeInFinalMenu(true).build(),
                ProductPlanDto.builder().productId("JP01").productName("Ramen Tonkotsu").finalDisplayPrice(120000.0).displayGroup("FEATURED").includeInFinalMenu(true).build()
        ));
        plan.setCategories(List.of(mainCourse));

        // 1. Verify dynamic context injection has exact products and prices
        String contextJson = generator.buildBusinessContext(plan);
        assertTrue(contextJson.contains("Pizza Seafood"), "Context phải chứa Pizza Seafood");
        assertTrue(contextJson.contains("189000"), "Context phải chứa giá 189000");
        assertTrue(contextJson.contains("Ramen Tonkotsu"), "Context phải chứa Ramen Tonkotsu");
        assertTrue(contextJson.contains("120000"), "Context phải chứa giá 120000");

        // 2. Generate artwork under TRE_TRUNG visual style
        AiMenuArtworkResultDto result = generator.generateArtwork(plan, "TRE_TRUNG");
        assertTrue(result.isSuccess());
        assertNotNull(result.getImageUrl());

        // 3. Verify manifest & business integrity validation
        ArtworkValidationResultDto valResult = validator.validateArtworkContent(plan, result.getRenderManifest(), result.getImageUrl());
        assertTrue(valResult.isValid(), "Validation phải PASSED cho dataset món Nhật/Ý dưới style TRE_TRUNG");
        assertEquals(2, valResult.getMatchedItemsCount());
        assertEquals("PASSED", valResult.getStatus());
    }

    @Test
    @DisplayName("Reference Asset Guard Test: Missing Reference Image throws REFERENCE_ASSET_REQUIRED")
    void testReferenceAssetRequiredException() {
        MenuStyleConfigDto invalidConfig = MenuStyleConfigDto.builder()
                .styleId("INVALID_NON_EXISTENT_STYLE")
                .styleName("Invalid Style")
                .referenceImagePath("classpath:/menu-style/NON_EXISTENT/reference.png")
                .build();

        MenuStyleConfigRegistry customRegistry = new MenuStyleConfigRegistry() {
            @Override
            public MenuStyleConfigDto getStyleConfig(String styleId) {
                if ("INVALID_NON_EXISTENT_STYLE".equals(styleId)) {
                    return invalidConfig;
                }
                return super.getStyleConfig(styleId);
            }
        };

        org.springframework.web.client.RestTemplate mockAi = createMockAiRestTemplate(SAMPLE_ARTWORK_BASE64);
        AiMenuArtworkGeneratorService artworkGen = new AiMenuArtworkGeneratorService(mockAi, new ObjectMapper(), customRegistry);
        AiFinalMenuDesignerService customDesignerService = new AiFinalMenuDesignerService(new ObjectMapper(), customRegistry, artworkGen, new ArtworkContentValidatorService());

        FinalMenuPlanDto plan = new FinalMenuPlanDto();
        plan.setSessionId("99");

        Exception ex = assertThrows(IllegalStateException.class, () -> {
            customDesignerService.generateStructuredSvgDesign(plan, "INVALID_NON_EXISTENT_STYLE", null);
        });

        assertTrue(ex.getMessage().contains("REFERENCE_ASSET_REQUIRED"), "Ngoại lệ phải chứa thông điệp REFERENCE_ASSET_REQUIRED");
    }

    @Test
    @DisplayName("Content Validation Level 1 Test: Accurate Manifest matching Business Truth produces PASSED status")
    void testContentValidationAccurateManifest() {
        ArtworkContentValidatorService validator = new ArtworkContentValidatorService();

        FinalMenuPlanDto plan = new FinalMenuPlanDto();
        plan.setSessionId("999");
        CategoryPlanDto coffeeCat = new CategoryPlanDto();
        coffeeCat.setCategoryName("Coffee");
        coffeeCat.setProducts(List.of(
                ProductPlanDto.builder().productId("P01").productName("Cafe Latte").finalDisplayPrice(45000.0).displayGroup("HERO").includeInFinalMenu(true).build(),
                ProductPlanDto.builder().productId("P02").productName("Americano").finalDisplayPrice(35000.0).displayGroup("CORE").includeInFinalMenu(true).build()
        ));
        plan.setCategories(List.of(coffeeCat));

        AiArtworkManifestDto accurateManifest = AiArtworkManifestDto.builder()
                .categories(List.of(
                        AiArtworkManifestDto.ManifestCategoryDto.builder()
                                .name("Coffee")
                                .products(List.of(
                                         AiArtworkManifestDto.ManifestProductDto.builder().name("Cafe Latte").price(45000.0).build(),
                                         AiArtworkManifestDto.ManifestProductDto.builder().name("Americano").price(35000.0).build()
                                ))
                                .build()
                ))
                .build();

        ArtworkValidationResultDto result = validator.validateArtworkContent(plan, accurateManifest, "/generated/menu_999.png");

        assertTrue(result.isValid(), "Validation phải hợp lệ khi manifest khớp 100% với Business Truth");
        assertEquals("PASSED", result.getStatus());
        assertEquals(2, result.getMatchedItemsCount());
        assertTrue(result.getDiscrepancies().isEmpty());
    }

    @Test
    @DisplayName("Content Validation Level 1 Test: Price mismatch or missing product triggers REJECTED status")
    void testContentValidationPriceMismatchAndMissingProduct() {
        ArtworkContentValidatorService validator = new ArtworkContentValidatorService();

        FinalMenuPlanDto plan = new FinalMenuPlanDto();
        plan.setSessionId("999");
        CategoryPlanDto coffeeCat = new CategoryPlanDto();
        coffeeCat.setCategoryName("Coffee");
        coffeeCat.setProducts(List.of(
                ProductPlanDto.builder().productId("P01").productName("Cafe Latte").finalDisplayPrice(45000.0).displayGroup("HERO").includeInFinalMenu(true).build(),
                ProductPlanDto.builder().productId("P02").productName("Americano").finalDisplayPrice(35000.0).displayGroup("CORE").includeInFinalMenu(true).build()
        ));
        plan.setCategories(List.of(coffeeCat));

        // Manifest has incorrect price for Cafe Latte (50000 instead of 45000) and misses Americano
        AiArtworkManifestDto inaccurateManifest = AiArtworkManifestDto.builder()
                .categories(List.of(
                        AiArtworkManifestDto.ManifestCategoryDto.builder()
                                .name("Coffee")
                                .products(List.of(
                                        AiArtworkManifestDto.ManifestProductDto.builder().name("Cafe Latte").price(50000.0).build()
                                ))
                                .build()
                ))
                .build();

        ArtworkValidationResultDto result = validator.validateArtworkContent(plan, inaccurateManifest, "/generated/menu_999.png");

        assertFalse(result.isValid(), "Validation bắt buộc phải REJECT khi có sai lệch giá hoặc thiếu món");
        assertEquals("REJECTED", result.getStatus());
        assertEquals(2, result.getDiscrepancies().size());
        assertTrue(result.getDiscrepancies().stream().anyMatch(d -> d.contains("PRICE_MISMATCH")));
        assertTrue(result.getDiscrepancies().stream().anyMatch(d -> d.contains("MISSING_PRODUCT")));
    }

    @Test
    @DisplayName("Acceptance Test Dataset A (Coffee Shop): AI Menu dynamically generates with Cafe Latte, Americano, Tea Peach")
    void testDynamicBusinessContextInjectionDatasetA_CoffeeShop() {
        org.springframework.web.client.RestTemplate mockAi = createMockAiRestTemplate(SAMPLE_ARTWORK_BASE64);
        AiMenuArtworkGeneratorService generator = new AiMenuArtworkGeneratorService(mockAi, new ObjectMapper(), new MenuStyleConfigRegistry());
        ArtworkContentValidatorService validator = new ArtworkContentValidatorService();

        FinalMenuPlanDto plan = new FinalMenuPlanDto();
        plan.setSessionId("1001");
        CategoryPlanDto coffeeCat = new CategoryPlanDto();
        coffeeCat.setCategoryName("Coffee & Tea");
        coffeeCat.setProducts(List.of(
                ProductPlanDto.builder().productId("CF01").productName("Cafe Latte").finalDisplayPrice(45000.0).displayGroup("HERO").includeInFinalMenu(true).build(),
                ProductPlanDto.builder().productId("CF02").productName("Americano").finalDisplayPrice(35000.0).displayGroup("CORE").includeInFinalMenu(true).build(),
                ProductPlanDto.builder().productId("CF03").productName("Tea Peach").finalDisplayPrice(40000.0).displayGroup("FEATURED").includeInFinalMenu(true).build()
        ));
        plan.setCategories(List.of(coffeeCat));

        // 1. Verify dynamic business context JSON injection
        String contextJson = generator.buildBusinessContext(plan);
        assertTrue(contextJson.contains("Cafe Latte"), "Context JSON phải chứa Cafe Latte");
        assertTrue(contextJson.contains("Americano"), "Context JSON phải chứa Americano");
        assertTrue(contextJson.contains("Tea Peach"), "Context JSON phải chứa Tea Peach");

        // 2. Generate artwork
        AiMenuArtworkResultDto result = generator.generateArtwork(plan, "HIEN_DAI");
        assertTrue(result.isSuccess());
        assertNotNull(result.getImageUrl());
        assertTrue(result.getImageUrl().startsWith("data:image/png;base64,"));

        // 3. Verify content validation matches 100%
        ArtworkValidationResultDto valResult = validator.validateArtworkContent(plan, result.getRenderManifest(), result.getImageUrl());
        assertTrue(valResult.isValid());
        assertEquals("PASSED", valResult.getStatus());
        assertEquals(3, valResult.getMatchedItemsCount());
    }

    @Test
    @DisplayName("Acceptance Test Dataset B (Restaurant): AI Menu dynamically adapts to Pizza Seafood, Pasta, Dessert without code changes")
    void testDynamicBusinessContextInjectionDatasetB_Restaurant() {
        org.springframework.web.client.RestTemplate mockAi = createMockAiRestTemplate(SAMPLE_ARTWORK_BASE64);
        AiMenuArtworkGeneratorService generator = new AiMenuArtworkGeneratorService(mockAi, new ObjectMapper(), new MenuStyleConfigRegistry());
        ArtworkContentValidatorService validator = new ArtworkContentValidatorService();

        FinalMenuPlanDto plan = new FinalMenuPlanDto();
        plan.setSessionId("1002");
        CategoryPlanDto mainCat = new CategoryPlanDto();
        mainCat.setCategoryName("Italian Mains");
        mainCat.setProducts(List.of(
                ProductPlanDto.builder().productId("RS01").productName("Pizza Seafood").finalDisplayPrice(189000.0).displayGroup("HERO").includeInFinalMenu(true).build(),
                ProductPlanDto.builder().productId("RS02").productName("Pasta Carbonara").finalDisplayPrice(129000.0).displayGroup("FEATURED").includeInFinalMenu(true).build(),
                ProductPlanDto.builder().productId("RS03").productName("Dessert Tiramisu").finalDisplayPrice(65000.0).displayGroup("CORE").includeInFinalMenu(true).build()
        ));
        plan.setCategories(List.of(mainCat));

        // 1. Verify dynamic business context JSON injection
        String contextJson = generator.buildBusinessContext(plan);
        assertTrue(contextJson.contains("Pizza Seafood"), "Context JSON phải chứa Pizza Seafood");
        assertTrue(contextJson.contains("Pasta Carbonara"), "Context JSON phải chứa Pasta Carbonara");
        assertTrue(contextJson.contains("Dessert Tiramisu"), "Context JSON phải chứa Dessert Tiramisu");

        // 2. Generate artwork
        AiMenuArtworkResultDto result = generator.generateArtwork(plan, "SANG_TRONG");
        assertTrue(result.isSuccess());
        assertNotNull(result.getImageUrl());
        assertTrue(result.getImageUrl().startsWith("data:image/png;base64,"));

        // 3. Verify content validation matches 100%
        ArtworkValidationResultDto valResult = validator.validateArtworkContent(plan, result.getRenderManifest(), result.getImageUrl());
        assertTrue(valResult.isValid());
        assertEquals("PASSED", valResult.getStatus());
        assertEquals(3, valResult.getMatchedItemsCount());
    }
}

