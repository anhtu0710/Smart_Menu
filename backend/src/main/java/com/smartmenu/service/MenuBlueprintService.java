package com.smartmenu.service;

import com.smartmenu.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MenuBlueprintService {

    /**
     * Tạo VisualMenuBlueprint từ Dữ liệu Sản phẩm thực tế, MenuRestructurePlan và BusinessOptimizationResult
     * Tuân thủ 100% Nguyên tắc Validation:
     * - Không hard-code
     * - Chỉ chấp nhận các món thực sự tồn tại trong products của session
     * - Giữ nguyên Brand Identity thực tế (không bịa tên giả)
     * - Đóng gói layout logic cố định có version = 1
     */
    public VisualMenuBlueprintDto buildBlueprint(
            List<ProductItemDto> products,
            MenuRestructurePlanDto plan,
            BusinessOptimizationResultDto optimizationResult,
            String detectedBrandName
    ) {
        return buildBlueprint(null, products, plan, optimizationResult, detectedBrandName, null, null);
    }

    public VisualMenuBlueprintDto buildBlueprint(
            Long sessionId,
            List<ProductItemDto> products,
            MenuRestructurePlanDto plan,
            BusinessOptimizationResultDto optimizationResult,
            String detectedBrandName,
            String logoUrl,
            String tagline
    ) {
        if (products == null || products.isEmpty()) {
            log.warn("Danh sách sản phẩm trống, không thể tạo Blueprint cho Session {}", sessionId);
            return VisualMenuBlueprintDto.builder()
                    .version(1)
                    .sessionId(sessionId)
                    .brandName(detectedBrandName != null && !detectedBrandName.isBlank() ? detectedBrandName : "Chưa xác định được thông tin thương hiệu")
                    .logoUrl(logoUrl)
                    .tagline(tagline)
                    .heroDishes(Collections.emptyList())
                    .coreDishes(Collections.emptyList())
                    .categoryOrder(Collections.emptyList())
                    .reducedPriorityItems(Collections.emptyList())
                    .comboPlacements(Collections.emptyList())
                    .structuredCombos(Collections.emptyList())
                    .priceRecommendations(Collections.emptyList())
                    .imagePriority(Collections.emptyList())
                    .sections(Collections.emptyList())
                    .layoutSummary("Chưa có dữ liệu sản phẩm")
                    .build();
        }

        // Map tên sản phẩm (viết thường + trim) -> ProductItemDto
        Map<String, ProductItemDto> productMap = new HashMap<>();
        for (ProductItemDto p : products) {
            if (p != null && p.getName() != null) {
                productMap.put(p.getName().trim().toLowerCase(), p);
            }
        }

        // 1. Map Hero Dishes
        List<ProductItemDto> heroDishes = findMatchingProducts(plan != null ? plan.getHeroDishes() : null, productMap);

        // 2. Map Core Dishes
        List<ProductItemDto> coreDishes = findMatchingProducts(plan != null ? plan.getCoreDishes() : null, productMap);

        // 3. Map Reduced Priority Items (Giữ nguyên thông tin món, không bị xóa)
        List<ProductItemDto> reducedPriorityItems = findMatchingProducts(plan != null ? plan.getReducedPriorityItems() : null, productMap);

        // 4. Map Image Priority Items
        List<ProductItemDto> imagePriority = findMatchingProducts(plan != null ? plan.getImagePriority() : null, productMap);

        // 5. Combos & Price Recommendations từ BusinessOptimizationService
        List<PriceRecommendationDto> priceRecommendations = (optimizationResult != null && optimizationResult.getPriceRecommendations() != null)
                ? optimizationResult.getPriceRecommendations()
                : Collections.emptyList();

        List<ComboPlacementDto> structuredCombos = (optimizationResult != null && optimizationResult.getStructuredCombos() != null)
                ? optimizationResult.getStructuredCombos()
                : Collections.emptyList();

        List<String> comboPlacements = new ArrayList<>();
        for (ComboPlacementDto combo : structuredCombos) {
            comboPlacements.add(combo.getComboName() + " (" + String.format("%,.0f", combo.getComboPrice()) + "đ)");
        }
        if (plan != null && plan.getComboPlacements() != null) {
            for (String c : plan.getComboPlacements()) {
                if (!comboPlacements.contains(c)) comboPlacements.add(c);
            }
        }

        // 6. Xử lý Thứ tự Danh mục (Category Order)
        List<String> rawCategoryOrder = (plan != null && plan.getCategoryOrder() != null)
                ? plan.getCategoryOrder()
                : Collections.emptyList();

        Map<String, List<ProductItemDto>> productsByCategory = new LinkedHashMap<>();
        for (ProductItemDto p : products) {
            String category = (p.getCategory() != null && !p.getCategory().isBlank())
                    ? p.getCategory().trim()
                    : "Danh mục khác";
            productsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(p);
        }

        List<String> finalCategoryOrder = new ArrayList<>();
        Set<String> processedCategories = new HashSet<>();

        for (String catOrder : rawCategoryOrder) {
            for (String actualCat : productsByCategory.keySet()) {
                if (!processedCategories.contains(actualCat) &&
                        (actualCat.equalsIgnoreCase(catOrder.trim()) || actualCat.toLowerCase().contains(catOrder.trim().toLowerCase()))) {
                    finalCategoryOrder.add(actualCat);
                    processedCategories.add(actualCat);
                    break;
                }
            }
        }

        for (String actualCat : productsByCategory.keySet()) {
            if (!processedCategories.contains(actualCat)) {
                finalCategoryOrder.add(actualCat);
                processedCategories.add(actualCat);
            }
        }

        Set<String> reducedDishNames = reducedPriorityItems.stream()
                .map(p -> p.getName().trim().toLowerCase())
                .collect(Collectors.toSet());

        Set<String> heroDishNames = heroDishes.stream()
                .map(p -> p.getName().trim().toLowerCase())
                .collect(Collectors.toSet());

        Set<String> coreDishNames = coreDishes.stream()
                .map(p -> p.getName().trim().toLowerCase())
                .collect(Collectors.toSet());

        // 7. Tạo Sections
        List<BlueprintSectionDto> sections = new ArrayList<>();
        for (String catName : finalCategoryOrder) {
            List<ProductItemDto> catProducts = productsByCategory.getOrDefault(catName, Collections.emptyList());
            if (catProducts.isEmpty()) continue;

            List<ProductItemDto> sortedDishes = new ArrayList<>(catProducts);
            sortedDishes.sort((a, b) -> {
                String nameA = a.getName().trim().toLowerCase();
                String nameB = b.getName().trim().toLowerCase();

                int scoreA = getItemPriorityScore(nameA, heroDishNames, coreDishNames, reducedDishNames);
                int scoreB = getItemPriorityScore(nameB, heroDishNames, coreDishNames, reducedDishNames);

                return Integer.compare(scoreB, scoreA);
            });

            String sectionPriority = "NORMAL";
            if (catProducts.stream().anyMatch(p -> heroDishNames.contains(p.getName().trim().toLowerCase()))) {
                sectionPriority = "HERO";
            } else if (catProducts.stream().anyMatch(p -> coreDishNames.contains(p.getName().trim().toLowerCase()))) {
                sectionPriority = "CORE";
            }

            sections.add(BlueprintSectionDto.builder()
                    .categoryName(catName)
                    .priority(sectionPriority)
                    .dishes(sortedDishes)
                    .build());
        }

        String summary = (plan != null && plan.getFinalLayoutSummary() != null && !plan.getFinalLayoutSummary().isBlank())
                ? plan.getFinalLayoutSummary()
                : "Bố cục menu mới được tối ưu dựa trên phân tích hiệu suất món ăn và vị trí ưu tiên.";

        String finalBrandName = (detectedBrandName != null && !detectedBrandName.isBlank())
                ? detectedBrandName
                : "Chưa xác định được thông tin thương hiệu";

        log.info("Đã tạo VisualMenuBlueprint (version 1) cho thương hiệu '{}' thành công với {} giá đề xuất",
                finalBrandName, priceRecommendations.size());

        return VisualMenuBlueprintDto.builder()
                .version(1)
                .sessionId(sessionId)
                .brandName(finalBrandName)
                .logoUrl(logoUrl)
                .tagline(tagline)
                .heroDishes(heroDishes)
                .coreDishes(coreDishes)
                .priorityProducts(plan != null ? plan.getPriorityProducts() : Collections.emptyList())
                .categoryOrder(finalCategoryOrder)
                .reducedPriorityItems(reducedPriorityItems)
                .comboPlacements(comboPlacements)
                .structuredCombos(structuredCombos)
                .priceRecommendations(priceRecommendations)
                .imagePriority(imagePriority)
                .sections(sections)
                .layoutSummary(summary)
                .build();
    }

    private List<ProductItemDto> findMatchingProducts(
            List<String> dishNames,
            Map<String, ProductItemDto> productMap
    ) {
        if (dishNames == null || dishNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductItemDto> result = new ArrayList<>();
        Set<String> added = new HashSet<>();

        for (String rawName : dishNames) {
            if (rawName == null || rawName.isBlank()) continue;
            String key = rawName.trim().toLowerCase();

            if (productMap.containsKey(key)) {
                ProductItemDto p = productMap.get(key);
                if (!added.contains(p.getName())) {
                    result.add(p);
                    added.add(p.getName());
                }
            } else {
                for (Map.Entry<String, ProductItemDto> entry : productMap.entrySet()) {
                    if (entry.getKey().contains(key) || key.contains(entry.getKey())) {
                        ProductItemDto p = entry.getValue();
                        if (!added.contains(p.getName())) {
                            result.add(p);
                            added.add(p.getName());
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private int getItemPriorityScore(
            String name,
            Set<String> heroNames,
            Set<String> coreNames,
            Set<String> reducedNames
    ) {
        if (heroNames.contains(name)) return 100;
        if (coreNames.contains(name)) return 80;
        if (reducedNames.contains(name)) return 10;
        return 50;
    }
}
