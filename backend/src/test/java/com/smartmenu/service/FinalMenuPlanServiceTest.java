package com.smartmenu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmenu.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinalMenuPlanServiceTest {

    private BusinessAnalysisService businessAnalysisService;
    private BusinessOptimizationService businessOptimizationService;
    private FinalMenuPlanService finalMenuPlanService;
    private FinalMenuPlanValidatorService validatorService;

    @BeforeEach
    void setUp() {
        businessAnalysisService = new BusinessAnalysisService();
        businessOptimizationService = new BusinessOptimizationService();
        ObjectMapper objectMapper = new ObjectMapper();
        finalMenuPlanService = new FinalMenuPlanService(objectMapper);
        validatorService = new FinalMenuPlanValidatorService();
    }

    @Test
    @DisplayName("Acceptance Test 1: HERO Display Intent & Layout Constraints Assignment")
    void testAcceptanceTest1HeroDisplayIntent() {
        List<ProductItemDto> products = new ArrayList<>();
        // Product A: High Quantity + High Margin -> BEST_SELLER + HIGH_PROFIT -> HERO
        products.add(ProductItemDto.builder().id("PA1").name("Item Alpha").category("Cat A").originalPrice(50000.0).costPrice(15000.0).salesQuantity(500).build());
        // Product B: Low Quantity + Low Margin -> REDUCED_PRIORITY
        products.add(ProductItemDto.builder().id("PA2").name("Item Beta").category("Cat A").originalPrice(40000.0).costPrice(38000.0).salesQuantity(10).build());

        List<CategoryRevenueDto> catRevs = new ArrayList<>();
        AnalysisReportDto report = businessAnalysisService.analyzeBusinessData(products, catRevs);
        BusinessOptimizationResultDto opt = businessOptimizationService.optimizeBusinessData(1001L, products, report, catRevs);

        FinalMenuPlanDto plan = finalMenuPlanService.createAndFreezeFinalMenuPlan(1001L, products, report, opt, catRevs);

        assertTrue(validatorService.validatePlan(plan));

        ProductPlanDto heroItem = plan.getCategories().get(0).getProducts().stream()
                .filter(p -> "Item Alpha".equals(p.getProductName()))
                .findFirst().orElseThrow();

        assertEquals("HERO", heroItem.getDisplayGroup());
        assertEquals("LARGE", heroItem.getDisplaySize());
        assertEquals("TOP", heroItem.getDisplayPosition());
        assertEquals("MAIN_ATTENTION", heroItem.getDisplayPurpose());
        assertNotNull(heroItem.getLayoutConstraint());
        assertEquals("HERO_BANNER", heroItem.getLayoutConstraint().getSectionType());
    }

    @Test
    @DisplayName("Acceptance Test 2: Excluded Products strictly in excludedProductIds & absent from categories.products")
    void testAcceptanceTest2ExcludedProducts() {
        List<ProductItemDto> products = new ArrayList<>();
        products.add(ProductItemDto.builder().id("PX1").name("Món Chính Bán Bán Chạy").category("Nhóm A").originalPrice(100000.0).costPrice(30000.0).salesQuantity(300).build());
        products.add(ProductItemDto.builder().id("PX2").name("Món Phụ Trung Bình").category("Nhóm A").originalPrice(60000.0).costPrice(25000.0).salesQuantity(100).build());
        products.add(ProductItemDto.builder().id("PX3").name("Món Yếu Hàng").category("Nhóm A").originalPrice(50000.0).costPrice(48000.0).salesQuantity(5).build()); // REMOVE

        List<CategoryRevenueDto> catRevs = new ArrayList<>();
        AnalysisReportDto report = businessAnalysisService.analyzeBusinessData(products, catRevs);
        BusinessOptimizationResultDto opt = businessOptimizationService.optimizeBusinessData(1002L, products, report, catRevs);

        FinalMenuPlanDto plan = finalMenuPlanService.createAndFreezeFinalMenuPlan(1002L, products, report, opt, catRevs);

        assertTrue(validatorService.validatePlan(plan));

        assertTrue(plan.getExcludedProductIds().contains("PX3"));

        boolean existsInCategories = plan.getCategories().stream()
                .flatMap(c -> c.getProducts().stream())
                .anyMatch(p -> "PX3".equalsIgnoreCase(p.getProductId()));

        assertFalse(existsInCategories, "Sản phẩm bị REMOVE không được xuất hiện trong categories.products");
    }

    @Test
    @DisplayName("Acceptance Test 3: Combo Strategy & Embedded Real Product Objects")
    void testAcceptanceTest3ComboStrategy() {
        List<ProductItemDto> products = new ArrayList<>();
        // Product 1: Best Seller
        products.add(ProductItemDto.builder().id("PC1").name("Món Chính Hot").category("Món Chính").originalPrice(150000.0).costPrice(40000.0).salesQuantity(400).build());
        // Product 2: Reduced Quantity but Good Margin -> COMBO candidate
        products.add(ProductItemDto.builder().id("PC2").name("Món Phụ Margin Tốt").category("Món Chính").originalPrice(80000.0).costPrice(20000.0).salesQuantity(20).build());
        // Product 3: Reduced Quantity & Low Margin -> REMOVE
        products.add(ProductItemDto.builder().id("PC3").name("Món Phụ Margin Thấp").category("Món Chính").originalPrice(50000.0).costPrice(48000.0).salesQuantity(5).build());

        List<CategoryRevenueDto> catRevs = new ArrayList<>();
        AnalysisReportDto report = businessAnalysisService.analyzeBusinessData(products, catRevs);
        BusinessOptimizationResultDto opt = businessOptimizationService.optimizeBusinessData(1003L, products, report, catRevs);
        if (opt.getCombos() == null || opt.getCombos().isEmpty()) {
            opt.setCombos(new ArrayList<>(List.of(
                    ComboRecommendationDto.builder()
                            .comboId("CB001")
                            .mainProductId("PC1")
                            .mainProductName("Món Chính Hot")
                            .secondaryProductId("PC2")
                            .secondaryProductName("Món Phụ Margin Tốt")
                            .comboName("Combo Món Chính Hot + Món Phụ Margin Tốt")
                            .comboPrice(200000L)
                            .reason("Combo kích cầu")
                            .build()
            )));
        }

        FinalMenuPlanDto plan = finalMenuPlanService.createAndFreezeFinalMenuPlan(1003L, products, report, opt, catRevs);

        assertTrue(validatorService.validatePlan(plan));
        assertNotNull(plan.getCombos());
        assertFalse(plan.getCombos().isEmpty());

        ComboPlanDto combo = plan.getCombos().get(0);
        assertEquals("COMBO_HIGHLIGHT", combo.getDisplayGroup());
        assertEquals("MEDIUM", combo.getDisplaySize());
        assertEquals("SECONDARY_ZONE", combo.getDisplayPosition());

        assertNotNull(combo.getProducts());
        assertFalse(combo.getProducts().isEmpty());
        assertTrue(combo.getProducts().stream().anyMatch(p -> "PC1".equals(p.getProductId())));
    }

    @Test
    @DisplayName("Acceptance Test 4: Unknown Dynamic Datasets - JSON Structure Adapts 100% Numerically")
    void testAcceptanceTest4UnknownDynamicDatasets() {
        // Dataset A: Quán Lẩu Nướng
        List<ProductItemDto> datasetA = new ArrayList<>();
        datasetA.add(ProductItemDto.builder().id("L1").name("Bò Mỹ Nướng").category("Nướng").originalPrice(200000.0).costPrice(60000.0).salesQuantity(600).build());
        datasetA.add(ProductItemDto.builder().id("L2").name("Bạch Tuộc Sa Tế").category("Nướng").originalPrice(150000.0).costPrice(50000.0).salesQuantity(350).build());
        datasetA.add(ProductItemDto.builder().id("L3").name("Rau Nấm Thêm").category("Nướng").originalPrice(30000.0).costPrice(28000.0).salesQuantity(10).build());

        List<CategoryRevenueDto> catRevsA = new ArrayList<>();
        AnalysisReportDto reportA = businessAnalysisService.analyzeBusinessData(datasetA, catRevsA);
        BusinessOptimizationResultDto optA = businessOptimizationService.optimizeBusinessData(2001L, datasetA, reportA, catRevsA);
        FinalMenuPlanDto planA = finalMenuPlanService.createAndFreezeFinalMenuPlan(2001L, datasetA, reportA, optA, catRevsA);

        assertTrue(validatorService.validatePlan(planA));
        assertEquals(1, planA.getExcludedProductIds().size());
        assertTrue(planA.getExcludedProductIds().contains("L3"));

        // Dataset B: Quán Sinh Tố & Tiệm Kem
        List<ProductItemDto> datasetB = new ArrayList<>();
        datasetB.add(ProductItemDto.builder().id("S1").name("Sinh Tố Bơ").category("Sinh Tố").originalPrice(45000.0).costPrice(12000.0).salesQuantity(800).build());
        datasetB.add(ProductItemDto.builder().id("S2").name("Kem Dừa Côn Đảo").category("Kem").originalPrice(50000.0).costPrice(15000.0).salesQuantity(500).build());
        datasetB.add(ProductItemDto.builder().id("S3").name("Nước Ép Cần Tây").category("Sinh Tố").originalPrice(35000.0).costPrice(33000.0).salesQuantity(5).build());

        List<CategoryRevenueDto> catRevsB = new ArrayList<>();
        AnalysisReportDto reportB = businessAnalysisService.analyzeBusinessData(datasetB, catRevsB);
        BusinessOptimizationResultDto optB = businessOptimizationService.optimizeBusinessData(2002L, datasetB, reportB, catRevsB);
        FinalMenuPlanDto planB = finalMenuPlanService.createAndFreezeFinalMenuPlan(2002L, datasetB, reportB, optB, catRevsB);

        assertTrue(validatorService.validatePlan(planB));
        assertEquals(1, planB.getExcludedProductIds().size());
        assertTrue(planB.getExcludedProductIds().contains("S3"));
        assertEquals(2, planB.getCategories().size());
    }
}
