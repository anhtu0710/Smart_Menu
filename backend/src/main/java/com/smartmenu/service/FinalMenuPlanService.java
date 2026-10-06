package com.smartmenu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmenu.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinalMenuPlanService {

    private final ObjectMapper objectMapper;

    /**
     * MAPPING-ONLY & FREEZE SERVICE (V4):
     * Đọc kết quả từ BusinessOptimizationService & BusinessAnalysisService,
     * Chốt tầng Quyết Định Kinh Doanh (Business Decision) và tầng Chiến Lược Hiển Thị (Display Decision),
     * Đóng gói & FREEZE FinalMenuPlanDto bất biến (kèm planVersion & SHA-256 planHash).
     */
    public FinalMenuPlanDto createAndFreezeFinalMenuPlan(
            Long sessionId,
            List<ProductItemDto> rawProducts,
            AnalysisReportDto analysisReport,
            BusinessOptimizationResultDto optimizationResult,
            List<CategoryRevenueDto> categoryRevenues
    ) {
        String sessIdStr = sessionId != null ? String.valueOf(sessionId) : "0";

        if (optimizationResult == null || optimizationResult.getProducts() == null || optimizationResult.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Không có dữ liệu BusinessOptimizationResultDto hợp lệ để freeze FinalMenuPlan");
        }

        double totalRev = optimizationResult.getProducts().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIncludeInFinalMenu()))
                .mapToDouble(ProductOptimizationDto::getTotalRevenue).sum();

        List<CategoryOrderDto> catOrders = optimizationResult.getCategoryOrder() != null
                ? optimizationResult.getCategoryOrder()
                : Collections.emptyList();

        Map<String, List<ProductOptimizationDto>> prodsByCategory = optimizationResult.getProducts().stream()
                .collect(Collectors.groupingBy(ProductOptimizationDto::getCategoryName, LinkedHashMap::new, Collectors.toList()));

        List<CategoryPlanDto> finalCategories = new ArrayList<>();
        List<FinalMenuCategoryDto> legacyCategories = new ArrayList<>(); // Cho backward compatibility

        List<String> heroProductIds = new ArrayList<>();
        List<String> featuredProductIds = new ArrayList<>();
        List<String> coreProductIds = new ArrayList<>();
        List<String> secondaryProductIds = new ArrayList<>();
        List<String> excludedProductIds = new ArrayList<>();

        Map<String, ProductPlanDto> allProductPlanMap = new HashMap<>();

        for (CategoryOrderDto catDto : catOrders) {
            String catName = catDto.getCategoryName();
            String catId = catDto.getCategoryId();
            Integer catOrder = catDto.getCategoryOrder();

            List<ProductOptimizationDto> optProds = prodsByCategory.getOrDefault(catName, Collections.emptyList());

            // Active products included in final menu
            List<ProductOptimizationDto> activeOptProds = optProds.stream()
                    .filter(op -> Boolean.TRUE.equals(op.getIncludeInFinalMenu()))
                    .sorted(Comparator.comparing(op -> op.getDisplayOrder() != null ? op.getDisplayOrder() : 99))
                    .collect(Collectors.toList());

            double catRev = activeOptProds.stream().mapToDouble(ProductOptimizationDto::getTotalRevenue).sum();
            double displayWeight = totalRev > 0 ? Math.round((catRev / totalRev) * 1000.0) / 10.0 : 0.0;
            int maxProductsDisplay = activeOptProds.size();

            String catDisplayPosition = catOrder == 1 ? "TOP" : (catOrder <= 3 ? "MIDDLE" : "BOTTOM");
            String catLayoutType = catOrder == 1 ? "HERO_SECTION" : (catOrder <= 3 ? "FEATURED_GRID" : "STANDARD_LIST");

            LayoutConstraintDto catLayoutConstraint = LayoutConstraintDto.builder()
                    .canvasPriority(catOrder)
                    .sectionType(catOrder == 1 ? "HERO_BANNER" : (catOrder <= 3 ? "PRODUCT_GRID" : "LIST"))
                    .columns(catOrder <= 3 ? 2 : 1)
                    .maxRows(Math.max(1, (maxProductsDisplay + 1) / 2))
                    .areaPercentage(displayWeight)
                    .build();

            List<ProductPlanDto> categoryProductPlans = new ArrayList<>();
            List<FinalMenuProductDto> legacyProductDtos = new ArrayList<>();

            for (ProductOptimizationDto op : activeOptProds) {
                String pId = op.getProductId();
                List<String> tags = op.getBusinessTags() != null ? op.getBusinessTags() : Collections.emptyList();

                boolean isBestSeller = tags.contains(BusinessTag.BEST_SELLER.name());
                boolean isHighProfit = tags.contains(BusinessTag.HIGH_PROFIT.name());
                boolean isSpecialOffer = tags.contains(BusinessTag.SPECIAL_OFFER.name());

                String displayGroup;
                String displaySize;
                String displayPosition;
                String displayPurpose;
                String placementGroup;
                LayoutConstraintDto layoutConstraint;

                if (isBestSeller && isHighProfit) {
                    displayGroup = "HERO";
                    displaySize = "LARGE";
                    displayPosition = "TOP";
                    displayPurpose = "MAIN_ATTENTION";
                    placementGroup = "FEATURED";
                    layoutConstraint = LayoutConstraintDto.builder()
                            .canvasPriority(1)
                            .sectionType("HERO_BANNER")
                            .columns(1)
                            .maxRows(1)
                            .areaPercentage(30.0)
                            .build();
                    heroProductIds.add(pId);
                } else if (isBestSeller || isHighProfit || isSpecialOffer) {
                    displayGroup = "FEATURED";
                    displaySize = "MEDIUM";
                    displayPosition = "MIDDLE";
                    displayPurpose = "PROMINENT_CARD";
                    placementGroup = "FEATURED";
                    layoutConstraint = LayoutConstraintDto.builder()
                            .canvasPriority(2)
                            .sectionType("PRODUCT_GRID")
                            .columns(2)
                            .maxRows(2)
                            .areaPercentage(40.0)
                            .build();
                    featuredProductIds.add(pId);
                } else {
                    displayGroup = "CORE";
                    displaySize = "STANDARD";
                    displayPosition = "MIDDLE";
                    displayPurpose = "STANDARD_LIST";
                    placementGroup = "CORE";
                    layoutConstraint = LayoutConstraintDto.builder()
                            .canvasPriority(3)
                            .sectionType("LIST")
                            .columns(1)
                            .maxRows(4)
                            .areaPercentage(20.0)
                            .build();
                    coreProductIds.add(pId);
                }

                ProductPlanDto prodPlan = ProductPlanDto.builder()
                        .productId(pId)
                        .categoryId(catId)
                        .productName(op.getProductName())
                        .finalDisplayPrice((double) op.getFinalDisplayPrice())
                        .businessTags(tags)
                        .decision(op.getDecision())
                        .originalPrice((double) op.getOriginalPrice())
                        .recommendedPrice((double) op.getRecommendedPrice())
                        .profitMargin(op.getProfitMargin())
                        .salesQuantity(op.getSalesQuantity())
                        .displayGroup(displayGroup)
                        .displaySize(displaySize)
                        .displayPosition(displayPosition)
                        .displayPurpose(displayPurpose)
                        .layoutConstraint(layoutConstraint)
                        .displayPriority(op.getBusinessPriority())
                        .displayOrder(op.getDisplayOrder())
                        .globalDisplayOrder(op.getGlobalDisplayOrder())
                        .includeInFinalMenu(true)
                        .promoGift(op.getPromoGift())
                        .build();

                categoryProductPlans.add(prodPlan);
                allProductPlanMap.put(pId, prodPlan);

                // Legacy DTO map
                legacyProductDtos.add(FinalMenuProductDto.builder()
                        .productId(pId)
                        .categoryId(catId)
                        .productName(op.getProductName())
                        .originalPrice(op.getOriginalPrice())
                        .recommendedPrice(op.getRecommendedPrice())
                        .finalDisplayPrice(op.getFinalDisplayPrice())
                        .priceRecommendationReason(op.getReason())
                        .promoGift(op.getPromoGift())
                        .businessTags(tags)
                        .businessPriority(op.getBusinessPriority())
                        .placementGroup(placementGroup)
                        .displayPriority(op.getDisplayOrder() != null ? op.getDisplayOrder() : 99)
                        .displayOrder(op.getDisplayOrder())
                        .globalDisplayOrder(op.getGlobalDisplayOrder())
                        .includeInFinalMenu(true)
                        .salesQuantity(op.getSalesQuantity())
                        .profitMargin(op.getProfitMargin())
                        .build());
            }

            // Record EXCLUDED & COMBO item IDs
            for (ProductOptimizationDto op : optProds) {
                if (!Boolean.TRUE.equals(op.getIncludeInFinalMenu())) {
                    if (ProductDecision.REMOVE.name().equals(op.getDecision())) {
                        excludedProductIds.add(op.getProductId());
                    } else if (ProductDecision.COMBO.name().equals(op.getDecision())) {
                        secondaryProductIds.add(op.getProductId());
                    }
                }
            }

            finalCategories.add(CategoryPlanDto.builder()
                    .categoryId(catId)
                    .categoryName(catName)
                    .categoryOrder(catOrder)
                    .displayPriority(catOrder)
                    .displayWeight(displayWeight)
                    .maxProductsDisplay(maxProductsDisplay)
                    .displayPosition(catDisplayPosition)
                    .layoutType(catLayoutType)
                    .layoutConstraint(catLayoutConstraint)
                    .products(categoryProductPlans)
                    .build());

            legacyCategories.add(FinalMenuCategoryDto.builder()
                    .categoryId(catId)
                    .categoryName(catName)
                    .categoryOrder(catOrder)
                    .products(legacyProductDtos)
                    .build());
        }

        finalCategories.sort(Comparator.comparing(CategoryPlanDto::getCategoryOrder));
        legacyCategories.sort(Comparator.comparing(FinalMenuCategoryDto::getCategoryOrder));

        // 2. Map Combos kèm embedded full ProductPlanDto objects
        List<ComboPlanDto> finalComboPlans = new ArrayList<>();
        List<FinalMenuComboDto> legacyCombos = new ArrayList<>();
        List<String> comboIds = new ArrayList<>();

        if (optimizationResult.getCombos() != null) {
            int comboOrder = 1;
            for (ComboRecommendationDto c : optimizationResult.getCombos()) {
                List<String> prodIds = List.of(c.getMainProductId(), c.getSecondaryProductId());
                comboIds.add(c.getComboId());

                List<ProductPlanDto> comboEmbeddedProducts = new ArrayList<>();
                for (String pId : prodIds) {
                    ProductPlanDto found = allProductPlanMap.get(pId);
                    if (found == null) {
                        // Fallback: tìm từ raw products nếu là món bị COMBO
                        ProductItemDto rawP = rawProducts.stream().filter(rp -> pId.equalsIgnoreCase(rp.getId())).findFirst().orElse(null);
                        if (rawP != null) {
                            found = ProductPlanDto.builder()
                                    .productId(rawP.getId())
                                    .productName(rawP.getName())
                                    .finalDisplayPrice(rawP.getOriginalPrice())
                                    .businessTags(List.of("COMBO_CANDIDATE"))
                                    .decision("COMBO")
                                    .displayGroup("COMBO")
                                    .displaySize("MEDIUM")
                                    .displayPosition("SECONDARY_ZONE")
                                    .displayPurpose("COMBO_ITEM")
                                    .build();
                        }
                    }
                    if (found != null) {
                        comboEmbeddedProducts.add(found);
                    }
                }

                LayoutConstraintDto comboLayout = LayoutConstraintDto.builder()
                        .canvasPriority(4)
                        .sectionType("PRODUCT_GRID")
                        .columns(2)
                        .maxRows(1)
                        .areaPercentage(10.0)
                        .build();

                ComboPlanDto comboPlan = ComboPlanDto.builder()
                        .comboId(c.getComboId())
                        .comboName(c.getComboName())
                        .comboPrice((double) c.getComboPrice())
                        .reason(c.getReason())
                        .productIds(prodIds)
                        .products(comboEmbeddedProducts)
                        .displayGroup("COMBO_HIGHLIGHT")
                        .displaySize("MEDIUM")
                        .displayPosition("SECONDARY_ZONE")
                        .layoutConstraint(comboLayout)
                        .displayOrder(comboOrder++)
                        .build();

                finalComboPlans.add(comboPlan);

                legacyCombos.add(FinalMenuComboDto.builder()
                        .comboId(c.getComboId())
                        .comboName(c.getComboName())
                        .mainProductId(c.getMainProductId())
                        .mainProductName(c.getMainProductName())
                        .secondaryProductId(c.getSecondaryProductId())
                        .secondaryProductName(c.getSecondaryProductName())
                        .comboPrice(c.getComboPrice())
                        .reason(c.getReason())
                        .build());
            }
        }

        // 3. Display Strategy & Placement Strategy Summary DTOs
        List<String> catOrderNames = finalCategories.stream().map(CategoryPlanDto::getCategoryName).collect(Collectors.toList());

        DisplayStrategyDto displayStrategy = DisplayStrategyDto.builder()
                .categoryOrder(catOrderNames)
                .heroProductIds(heroProductIds)
                .featuredProductIds(featuredProductIds)
                .coreProductIds(coreProductIds)
                .comboIds(comboIds)
                .excludedProductIds(excludedProductIds)
                .rules(List.of(
                        "HERO_BANNER_FIRST",
                        "FEATURED_CARD_PROMINENT",
                        "CORE_AFTER_FEATURED",
                        "EXCLUDED_PRODUCTS_MUST_NOT_APPEAR"
                ))
                .build();

        FinalMenuPlacementStrategyDto placementStrategy = FinalMenuPlacementStrategyDto.builder()
                .featuredProductIds(featuredProductIds)
                .coreProductIds(coreProductIds)
                .secondaryProductIds(secondaryProductIds)
                .excludedProductIds(excludedProductIds)
                .rules(displayStrategy.getRules())
                .build();

        FinalMenuContentDto contentDto = FinalMenuContentDto.builder()
                .sessionId(sessIdStr)
                .categories(legacyCategories)
                .combos(legacyCombos)
                .build();

        // 4. Compute planHash SHA-256 Deterministic
        String planHash = computeDeterministicPlanHash(sessIdStr, finalCategories, finalComboPlans);
        String createdAtStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        FinalMenuPlanDto planDto = FinalMenuPlanDto.builder()
                .sessionId(sessIdStr)
                .planVersion(1)
                .planHash(planHash)
                .createdAt(createdAtStr)
                .categories(finalCategories)
                .combos(finalComboPlans)
                .excludedProductIds(excludedProductIds)
                .displayStrategy(displayStrategy)
                .analysisSummary(analysisReport)
                .finalMenuContent(contentDto)
                .placementStrategy(placementStrategy)
                .build();

        // AUDIT LOGGING CHO FINAL PLAN (V4 CONTRACT)
        List<String> normalProdsLog = finalCategories.stream()
                .flatMap(c -> c.getProducts().stream().map(p -> p.getProductName() + " [" + p.getDisplayGroup() + "] (" + p.getFinalDisplayPrice() + "đ)"))
                .collect(Collectors.toList());

        List<String> comboLog = finalComboPlans.stream()
                .map(c -> c.getComboName() + " (" + c.getComboPrice() + "đ)")
                .collect(Collectors.toList());

        log.info("[FINAL-PLAN V4] categoryOrder: {}", catOrderNames);
        log.info("[FINAL-PLAN V4] normalProducts (count={}): {}", normalProdsLog.size(), normalProdsLog);
        log.info("[FINAL-PLAN V4] combos (count={}): {}", comboLog.size(), comboLog);
        log.info("[FINAL-PLAN V4] excludedProductIds (count={}): {}", excludedProductIds.size(), excludedProductIds);

        log.info("[PLAN FREEZE SUCCESS] Đã tạo & Freeze FinalMenuPlan cho Session {} (planHash: {}, categories: {}, combos: {})",
                sessIdStr, planHash, finalCategories.size(), finalComboPlans.size());

        return planDto;
    }

    private String computeDeterministicPlanHash(String sessionId, List<CategoryPlanDto> categories, List<ComboPlanDto> combos) {
        try {
            Map<String, Object> canonicalMap = new TreeMap<>();
            canonicalMap.put("sessionId", sessionId);

            List<Map<String, Object>> catList = new ArrayList<>();
            for (CategoryPlanDto cat : categories) {
                Map<String, Object> cMap = new TreeMap<>();
                cMap.put("categoryId", cat.getCategoryId());
                cMap.put("categoryName", cat.getCategoryName());
                cMap.put("categoryOrder", cat.getCategoryOrder());

                List<Map<String, Object>> pList = new ArrayList<>();
                for (ProductPlanDto p : cat.getProducts()) {
                    Map<String, Object> pMap = new TreeMap<>();
                    pMap.put("productId", p.getProductId());
                    pMap.put("productName", p.getProductName());
                    pMap.put("finalDisplayPrice", p.getFinalDisplayPrice());
                    pMap.put("displayGroup", p.getDisplayGroup());
                    pMap.put("displaySize", p.getDisplaySize());
                    pList.add(pMap);
                }
                cMap.put("products", pList);
                catList.add(cMap);
            }
            canonicalMap.put("categories", catList);

            List<Map<String, Object>> comboList = new ArrayList<>();
            if (combos != null) {
                for (ComboPlanDto cb : combos) {
                    Map<String, Object> cbMap = new TreeMap<>();
                    cbMap.put("comboId", cb.getComboId());
                    cbMap.put("comboName", cb.getComboName());
                    cbMap.put("comboPrice", cb.getComboPrice());
                    comboList.add(cbMap);
                }
            }
            canonicalMap.put("combos", comboList);

            String canonicalJson = objectMapper.writeValueAsString(canonicalMap);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 16);

        } catch (Exception e) {
            log.warn("Không thể tính planHash SHA-256, sử dụng UUID fallback", e);
            return UUID.randomUUID().toString().substring(0, 16);
        }
    }
}
