package com.smartmenu.service;

import com.smartmenu.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FinalMenuPlanValidatorService {

    public static class FinalMenuPlanValidationException extends RuntimeException {
        public FinalMenuPlanValidationException(String message) {
            super(message);
        }
    }

    /**
     * Pre-AI Contract Validation Layer:
     * Kiểm tra toàn vẹn hợp đồng FinalMenuPlanDto trước khi gửi sang AI Designer.
     */
    public boolean validatePlan(FinalMenuPlanDto plan) {
        if (plan == null) {
            throw new FinalMenuPlanValidationException("FinalMenuPlanDto không được để trống (null)");
        }

        if (plan.getSessionId() == null || plan.getSessionId().isBlank()) {
            throw new FinalMenuPlanValidationException("sessionId không được để trống");
        }

        if (plan.getPlanHash() == null || plan.getPlanHash().isBlank()) {
            throw new FinalMenuPlanValidationException("planHash SHA-256 không được để trống");
        }

        List<CategoryPlanDto> categories = plan.getCategories();
        if (categories == null || categories.isEmpty()) {
            throw new FinalMenuPlanValidationException("Danh sách categories trong FinalMenuPlan không được để trống");
        }

        // 1. Rule 5: Category order phải độc nhất và liên tục 1, 2, 3...
        List<Integer> orders = categories.stream()
                .map(CategoryPlanDto::getCategoryOrder)
                .sorted()
                .collect(Collectors.toList());

        for (int i = 0; i < orders.size(); i++) {
            int expected = i + 1;
            if (orders.get(i) == null || orders.get(i) != expected) {
                throw new FinalMenuPlanValidationException(String.format("categoryOrder phải độc nhất và liên tục (Mong đợi %d nhưng gặp %s)", expected, orders.get(i)));
            }
        }

        Set<String> categoryNames = new HashSet<>();
        for (CategoryPlanDto cat : categories) {
            if (cat.getCategoryName() == null || cat.getCategoryName().isBlank()) {
                throw new FinalMenuPlanValidationException("Tên danh mục categoryName không được để trống");
            }
            if (!categoryNames.add(cat.getCategoryName().trim())) {
                throw new FinalMenuPlanValidationException("Trùng lặp tên danh mục: " + cat.getCategoryName());
            }
        }

        // 2. Rule 2: Món bị EXCLUDED / REMOVE không được phép xuất hiện trong categories.products
        List<String> excludedProductIds = plan.getExcludedProductIds() != null ? plan.getExcludedProductIds() : Collections.emptyList();
        Set<String> excludedSet = new HashSet<>(excludedProductIds);

        Set<String> displayedProductIds = new HashSet<>();

        for (CategoryPlanDto cat : categories) {
            List<ProductPlanDto> prods = cat.getProducts();
            if (prods == null) continue;

            Set<Integer> displayOrdersInCat = new HashSet<>();

            for (ProductPlanDto p : prods) {
                // Rule 7: Required fields
                if (p.getProductId() == null || p.getProductId().isBlank()) {
                    throw new FinalMenuPlanValidationException("productId không được để trống trong category: " + cat.getCategoryName());
                }
                if (p.getProductName() == null || p.getProductName().isBlank()) {
                    throw new FinalMenuPlanValidationException("productName không được để trống đối với ID: " + p.getProductId());
                }
                if (p.getFinalDisplayPrice() == null || p.getFinalDisplayPrice() <= 0) {
                    throw new FinalMenuPlanValidationException("finalDisplayPrice phải lớn hơn 0 đối với món: " + p.getProductName());
                }

                // Check excluded conflict
                if (excludedSet.contains(p.getProductId())) {
                    throw new FinalMenuPlanValidationException(String.format("Sản phẩm bị loại bỏ (REMOVE) ID=%s ('%s') tuyệt đối không được xuất hiện trong categories.products", p.getProductId(), p.getProductName()));
                }

                if (!displayedProductIds.add(p.getProductId())) {
                    throw new FinalMenuPlanValidationException("Trùng lặp productId hiển thị trong menu: " + p.getProductId());
                }

                // Rule 6: Display order không duplicate trong category
                if (p.getDisplayOrder() != null) {
                    if (!displayOrdersInCat.add(p.getDisplayOrder())) {
                        throw new FinalMenuPlanValidationException(String.format("Trùng lặp displayOrder=%d trong nhóm '%s'", p.getDisplayOrder(), cat.getCategoryName()));
                    }
                }

                // Rule 3: HERO product phải có HERO display intent & layout constraint
                boolean isBestSeller = p.getBusinessTags() != null && p.getBusinessTags().contains(BusinessTag.BEST_SELLER.name());
                boolean isHighProfit = p.getBusinessTags() != null && p.getBusinessTags().contains(BusinessTag.HIGH_PROFIT.name());

                if (isBestSeller && isHighProfit) {
                    if (!"HERO".equalsIgnoreCase(p.getDisplayGroup())) {
                        throw new FinalMenuPlanValidationException(String.format("Món vừa Best Seller vừa High Profit ('%s') bắt buộc phải có displayGroup = HERO", p.getProductName()));
                    }
                    if (!"LARGE".equalsIgnoreCase(p.getDisplaySize())) {
                        throw new FinalMenuPlanValidationException(String.format("Món HERO ('%s') bắt buộc phải có displaySize = LARGE", p.getProductName()));
                    }
                    if (p.getLayoutConstraint() == null || !"HERO_BANNER".equalsIgnoreCase(p.getLayoutConstraint().getSectionType())) {
                        throw new FinalMenuPlanValidationException(String.format("Món HERO ('%s') bắt buộc phải có sectionType = HERO_BANNER", p.getProductName()));
                    }
                }
            }
        }

        // 3. Rule 4: Combo validation
        List<ComboPlanDto> combos = plan.getCombos();
        if (combos != null) {
            for (ComboPlanDto combo : combos) {
                if (combo.getComboPrice() == null || combo.getComboPrice() <= 0) {
                    throw new FinalMenuPlanValidationException("Giá combo comboPrice phải lớn hơn 0 trong combo: " + combo.getComboName());
                }
                if (combo.getProductIds() == null || combo.getProductIds().isEmpty()) {
                    throw new FinalMenuPlanValidationException("Combo productIds không được để trống: " + combo.getComboName());
                }
                if (combo.getProducts() == null || combo.getProducts().isEmpty()) {
                    throw new FinalMenuPlanValidationException("Combo embedded products list không được để trống: " + combo.getComboName());
                }
            }
        }

        log.info("[PRE-AI CONTRACT VALIDATION SUCCESS] FinalMenuPlan cho Session {} (hash={}) đạt 100% hợp đồng Backend-AI",
                plan.getSessionId(), plan.getPlanHash());

        return true;
    }
}
