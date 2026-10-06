package com.smartmenu.service;

import com.smartmenu.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinalMenuPlanValidatorServiceTest {

    private FinalMenuPlanValidatorService validatorService;

    @BeforeEach
    void setUp() {
        validatorService = new FinalMenuPlanValidatorService();
    }

    @Test
    @DisplayName("Valid Plan Test: Should pass validation for clean FinalMenuPlanDto")
    void testValidPlan() {
        List<ProductPlanDto> products = new ArrayList<>();
        products.add(ProductPlanDto.builder()
                .productId("P001")
                .productName("Sản phẩm A")
                .finalDisplayPrice(50000.0)
                .businessTags(List.of("BEST_SELLER", "HIGH_PROFIT"))
                .displayGroup("HERO")
                .displaySize("LARGE")
                .layoutConstraint(LayoutConstraintDto.builder().sectionType("HERO_BANNER").build())
                .displayOrder(1)
                .build());

        CategoryPlanDto category = CategoryPlanDto.builder()
                .categoryId("CAT01")
                .categoryName("Nhóm A")
                .categoryOrder(1)
                .products(products)
                .build();

        FinalMenuPlanDto plan = FinalMenuPlanDto.builder()
                .sessionId("101")
                .planHash("hash123456789012")
                .categories(List.of(category))
                .excludedProductIds(List.of("P999"))
                .build();

        assertTrue(validatorService.validatePlan(plan));
    }

    @Test
    @DisplayName("Invalid Excluded Product Conflict Test: Should throw exception when REMOVE product is in categories.products")
    void testExcludedProductConflict() {
        List<ProductPlanDto> products = new ArrayList<>();
        products.add(ProductPlanDto.builder()
                .productId("P999") // Conflict with excludedProductIds
                .productName("Sản phẩm bị loại")
                .finalDisplayPrice(20000.0)
                .businessTags(List.of("REDUCED_PRIORITY"))
                .displayGroup("CORE")
                .displaySize("STANDARD")
                .displayOrder(1)
                .build());

        CategoryPlanDto category = CategoryPlanDto.builder()
                .categoryId("CAT01")
                .categoryName("Nhóm A")
                .categoryOrder(1)
                .products(products)
                .build();

        FinalMenuPlanDto plan = FinalMenuPlanDto.builder()
                .sessionId("102")
                .planHash("hash123456789012")
                .categories(List.of(category))
                .excludedProductIds(List.of("P999"))
                .build();

        assertThrows(FinalMenuPlanValidatorService.FinalMenuPlanValidationException.class, () -> {
            validatorService.validatePlan(plan);
        });
    }

    @Test
    @DisplayName("Discontinuous Category Order Test: Should throw exception when categoryOrder is discontinuous")
    void testDiscontinuousCategoryOrder() {
        CategoryPlanDto cat1 = CategoryPlanDto.builder().categoryId("C1").categoryName("Cat 1").categoryOrder(1).products(List.of()).build();
        CategoryPlanDto cat3 = CategoryPlanDto.builder().categoryId("C3").categoryName("Cat 3").categoryOrder(3).products(List.of()).build(); // Discontinuous order!

        FinalMenuPlanDto plan = FinalMenuPlanDto.builder()
                .sessionId("103")
                .planHash("hash123456789012")
                .categories(List.of(cat1, cat3))
                .build();

        assertThrows(FinalMenuPlanValidatorService.FinalMenuPlanValidationException.class, () -> {
            validatorService.validatePlan(plan);
        });
    }
}
