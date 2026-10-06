package com.smartmenu.service;

import com.smartmenu.dto.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BusinessOptimizationService {

    private static final List<String> PROMO_GIFTS = List.of(
            "Tặng Hướng dương",
            "Tặng Bò khô",
            "Tặng Bắp rang bơ",
            "Tặng Snack khoai tây"
    );

    @Data
    @Builder
    public static class CategoryBenchmark {
        private String categoryName;
        private double medianQuantity;
        private double medianRevenue;
        private double medianProfitMargin;
        private double averageQuantity;
        private double averageRevenue;
        private double averageMargin;
        private double averagePrice;
    }

    /**
     * Overloaded method cho tương thích ngược
     */
    public BusinessOptimizationResultDto optimizeBusinessData(List<ProductItemDto> products) {
        return optimizeBusinessData(null, products, null, null);
    }

    /**
     * PIPELINE BUSINESS OPTIMIZATION SERVICE (DYNAMIC ENGINE - 100% DATA-DRIVEN)
     *
     * 1. Tính toán Dynamic Category Benchmarks (Median Quantity, Median Revenue, Median Margin, Avg Price) cho từng nhóm
     * 2. Tagging Sản phẩm dựa thuần túy trên so sánh chỉ số với Benchmark của Category:
     *    - BEST_SELLER: quantity >= categoryMedianQuantity
     *    - HIGH_PROFIT: profitMargin >= categoryMedianProfitMargin
     *    - REDUCED_PRIORITY: quantity < categoryMedianQuantity AND revenue < categoryMedianRevenue AND profitMargin < categoryMedianProfitMargin
     *    - CORE_PRODUCT: Sản phẩm không thuộc 3 nhóm trên
     * 3. Decision Engine:
     *    - BEST_SELLER: Tăng 5% (làm tròn 1.000đ) nếu margin >= 30% & originalPrice <= categoryAveragePrice * 1.50
     *    - HIGH_PROFIT / CORE_PRODUCT: KEEP
     *    - REDUCED_PRIORITY: COMBO (nếu margin >= categoryMedianMargin) hoặc REMOVE (nếu margin < categoryMedianMargin)
     * 4. Log Audit chuẩn [CATEGORY-BENCHMARK] & [PRODUCT-OPTIMIZATION]
     */
    public BusinessOptimizationResultDto optimizeBusinessData(
            Long sessionId,
            List<ProductItemDto> products,
            AnalysisReportDto analysisReport,
            List<CategoryRevenueDto> categoryRevenueList
    ) {
        String sessIdStr = sessionId != null ? String.valueOf(sessionId) : "0";
        if (products == null || products.isEmpty()) {
            return BusinessOptimizationResultDto.builder()
                    .sessionId(sessIdStr)
                    .products(Collections.emptyList())
                    .priceRecommendations(Collections.emptyList())
                    .combos(Collections.emptyList())
                    .structuredCombos(Collections.emptyList())
                    .removeProductIds(Collections.emptyList())
                    .categoryOrder(Collections.emptyList())
                    .build();
        }

        double totalRevenue = products.stream().mapToDouble(ProductItemDto::getTotalRevenue).sum();
        double totalProfit = products.stream().mapToDouble(p -> (p.getOriginalPrice() - p.getCostPrice()) * p.getSalesQuantity()).sum();

        // 1. TÍNH DYNAMIC CATEGORY BENCHMARKS
        Map<String, CategoryBenchmark> categoryBenchmarks = calculateCategoryBenchmarks(products);

        // 2. RUN AUDIT LOGGING CHO CATEGORY BENCHMARKS
        for (CategoryBenchmark bench : categoryBenchmarks.values()) {
            log.info("[CATEGORY-BENCHMARK] Category: {} | Quantity Median: {} | Revenue Median: {} | Margin Median: {}% | Quantity Avg: {} | Revenue Avg: {} | Margin Avg: {}%",
                    bench.getCategoryName(),
                    round(bench.getMedianQuantity()),
                    round(bench.getMedianRevenue()),
                    round(bench.getMedianProfitMargin()),
                    round(bench.getAverageQuantity()),
                    round(bench.getAverageRevenue()),
                    round(bench.getAverageMargin()));
        }

        // 3. TAGGING & DECISION ENGINE TỪNG SẢN PHẨM
        List<ProductOptimizationDto> optProducts = new ArrayList<>();
        List<PriceRecommendationDto> priceRecommendations = new ArrayList<>();
        List<String> removeProductIds = new ArrayList<>();

        int prodIndex = 1;
        for (ProductItemDto p : products) {
            String catName = getCategoryName(p);
            String pId = (p.getId() != null && !p.getId().isBlank()) ? p.getId() : "P" + String.format("%03d", prodIndex++);

            CategoryBenchmark benchmark = categoryBenchmarks.get(catName);

            // Classify Tags thuần túy theo chỉ số Benchmark
            List<String> tags = classifyProductTags(p, benchmark);

            long originalPrice = Math.round(p.getOriginalPrice());
            long recommendedPrice = originalPrice;
            long finalDisplayPrice = originalPrice;
            String decision = ProductDecision.KEEP.name();
            boolean includeInFinalMenu = true;
            String reason = "Sản phẩm hoạt động bình thường.";

            double profit = (p.getOriginalPrice() - p.getCostPrice()) * p.getSalesQuantity();
            double margin = p.getProfitMargin();
            int qty = p.getSalesQuantity();

            int businessPriority = calculateBusinessPriority(tags);

            String promoGift = null;

            // DECISION ENGINE
            // 1. BEST SELLER: Tăng giá 5% nếu thỏa mãn guardrails
            if (tags.contains(BusinessTag.BEST_SELLER.name())) {
                decision = ProductDecision.KEEP.name();
                includeInFinalMenu = true;

                long calculatedRecPrice = Math.round((originalPrice * 1.05) / 1000.0) * 1000L;
                if (calculatedRecPrice <= originalPrice) {
                    calculatedRecPrice = originalPrice + 1000L;
                }

                // Guardrail: margin >= 30% & originalPrice <= benchmark.averagePrice * 1.50
                if (margin >= 30.0 && originalPrice <= benchmark.getAveragePrice() * 1.50 && calculatedRecPrice > originalPrice) {
                    decision = ProductDecision.PRICE_INCREASE.name();
                    recommendedPrice = calculatedRecPrice;
                    finalDisplayPrice = calculatedRecPrice;
                    reason = String.format("Sản phẩm bán chạy (Best Seller). Điều chỉnh giá từ %,dđ -> %,dđ (+5%%) để tối ưu lợi nhuận.",
                            originalPrice, finalDisplayPrice);

                    double changePercent = Math.round(((double)(finalDisplayPrice - originalPrice) / originalPrice) * 1000.0) / 10.0;
                    priceRecommendations.add(PriceRecommendationDto.builder()
                            .productId(pId)
                            .productName(p.getName().trim())
                            .categoryName(catName)
                            .currentPrice((double) originalPrice)
                            .recommendedPrice((double) finalDisplayPrice)
                            .priceChangePercent(changePercent)
                            .reason(reason)
                            .build());
                } else {
                    reason = "Sản phẩm bán chạy (Best Seller). Giữ nguyên giá hiện tại để bảo vệ lượng bán.";
                }
            }
            // 2. MÓN GIÁ CAO / BIÊN LỢI NHUẬN TỐT NHƯNG BÁN CHẬM: GIẢM GIÁ 10% + TẶNG KÈM ƯU ĐÃI
            else if (tags.contains(BusinessTag.SPECIAL_OFFER.name())) {
                decision = ProductDecision.PRICE_DECREASE.name();
                includeInFinalMenu = true;

                long calculatedRecPrice = Math.round((originalPrice * 0.90) / 1000.0) * 1000L;
                double cost = p.getCostPrice();
                // Guardrail: Giá sau giảm không thấp hơn giá vốn + 20%
                if (cost > 0 && calculatedRecPrice < cost * 1.20) {
                    calculatedRecPrice = Math.round((cost * 1.20) / 1000.0) * 1000L;
                }

                if (calculatedRecPrice < originalPrice) {
                    recommendedPrice = calculatedRecPrice;
                    finalDisplayPrice = calculatedRecPrice;
                } else {
                    recommendedPrice = originalPrice;
                    finalDisplayPrice = originalPrice;
                }

                promoGift = PROMO_GIFTS.get((prodIndex - 1) % PROMO_GIFTS.size());
                reason = String.format("Món giá cao/lợi nhuận tốt nhưng bán chậm. Giảm giá từ %,dđ -> %,dđ (-10%%) kèm ưu đãi [%s] để kích cầu.",
                        originalPrice, finalDisplayPrice, promoGift);

                double changePercent = Math.round(((double)(finalDisplayPrice - originalPrice) / originalPrice) * 1000.0) / 10.0;
                priceRecommendations.add(PriceRecommendationDto.builder()
                        .productId(pId)
                        .productName(p.getName().trim())
                        .categoryName(catName)
                        .currentPrice((double) originalPrice)
                        .recommendedPrice((double) finalDisplayPrice)
                        .priceChangePercent(changePercent)
                        .reason(reason)
                        .build());
            }
            // 3. MÓN BÁN KÉM, LỢI NHUẬN THẤP (DOG): LOẠI BỎ KHỎI MENU
            else if (tags.contains(BusinessTag.REDUCED_PRIORITY.name())) {
                decision = ProductDecision.REMOVE.name();
                includeInFinalMenu = false;
                if (!tags.contains(BusinessTag.REMOVE_CANDIDATE.name())) {
                    tags.add(BusinessTag.REMOVE_CANDIDATE.name());
                }
                reason = String.format("Sản phẩm doanh số thấp (%d), đóng góp doanh thu thấp và lợi nhuận thấp (%.1f%% < median %.1f%%). Đề xuất loại khỏi thực đơn.", qty, margin, benchmark.getMedianProfitMargin());
                removeProductIds.add(pId);
            }
            else if (tags.contains(BusinessTag.CORE_PRODUCT.name()) && ProductDecision.KEEP.name().equals(decision)) {
                reason = "Sản phẩm doanh số trung bình và lợi nhuận tốt, giữ lại trong menu.";
            }

            ProductOptimizationDto dto = ProductOptimizationDto.builder()
                    .productId(pId)
                    .productName(p.getName().trim())
                    .categoryId("CAT00")
                    .categoryName(catName)
                    .businessTags(tags)
                    .decision(decision)
                    .originalPrice(originalPrice)
                    .recommendedPrice(recommendedPrice)
                    .finalDisplayPrice(finalDisplayPrice)
                    .includeInFinalMenu(includeInFinalMenu)
                    .businessPriority(businessPriority)
                    .reason(reason)
                    .promoGift(promoGift)
                    .profitMargin(margin)
                    .salesQuantity(qty)
                    .totalRevenue(p.getTotalRevenue())
                    .estimatedProfit(profit)
                    .bcgCategory(p.getBcgCategory())
                    .build();

            optProducts.add(dto);

            // MANDATORY RUNTIME AUDIT LOG
            log.info("[PRODUCT-OPTIMIZATION] Product: {} | Quantity: {} | Category Median Quantity: {} | Revenue: {} | Category Median Revenue: {} | Margin: {}% | Category Median Margin: {}% | Tags: {} | Decision: {}",
                    p.getName().trim(),
                    qty,
                    round(benchmark.getMedianQuantity()),
                    round(p.getTotalRevenue()),
                    round(benchmark.getMedianRevenue()),
                    round(margin),
                    round(benchmark.getMedianProfitMargin()),
                    tags,
                    decision);
        }

        // 4. TẠO COMBO CANDIDATE TỪ CÁC SẢN PHẨM THẬT
        List<ComboRecommendationDto> combos = generateComboRecommendations(optProducts);

        List<ComboPlacementDto> structuredCombos = combos.stream()
                .map(c -> ComboPlacementDto.builder()
                        .mainProductId(c.getMainProductId())
                        .mainProductName(c.getMainProductName())
                        .pairedProductId(c.getSecondaryProductId())
                        .pairedProductName(c.getSecondaryProductName())
                        .comboName(c.getComboName())
                        .comboPrice((double) c.getComboPrice())
                        .reason(c.getReason())
                        .build())
                .collect(Collectors.toList());

        // 5. SẮP XẾP CATEGORY ORDER THEO PRIORITY KINH DOANH
        List<CategoryOrderDto> categoryOrderList = calculateCategoryOrder(optProducts, totalRevenue, totalProfit);

        Map<String, String> categoryIdMap = categoryOrderList.stream()
                .collect(Collectors.toMap(CategoryOrderDto::getCategoryName, CategoryOrderDto::getCategoryId, (a, b) -> a));
        Map<String, Integer> categoryOrderMap = categoryOrderList.stream()
                .collect(Collectors.toMap(CategoryOrderDto::getCategoryName, CategoryOrderDto::getCategoryOrder, (a, b) -> a));

        for (ProductOptimizationDto dto : optProducts) {
            dto.setCategoryId(categoryIdMap.getOrDefault(dto.getCategoryName(), "CAT01"));
            dto.setCategoryOrder(categoryOrderMap.getOrDefault(dto.getCategoryName(), 1));
        }

        // 6. SẮP XẾP VÀ CHỐT PRODUCT DISPLAY ORDER
        assignDisplayOrders(optProducts, categoryOrderList);

        log.info("[BUSINESS OPTIMIZATION SUCCESS] Session {}: Total={} | PriceIncrease={} | Combos={} | Removed={} | Categories={}",
                sessIdStr, optProducts.size(), priceRecommendations.size(), combos.size(), removeProductIds.size(), categoryOrderList.size());

        return BusinessOptimizationResultDto.builder()
                .sessionId(sessIdStr)
                .products(optProducts)
                .priceRecommendations(priceRecommendations)
                .combos(combos)
                .structuredCombos(structuredCombos)
                .removeProductIds(removeProductIds)
                .categoryOrder(categoryOrderList)
                .build();
    }

    /**
     * Tagging sản phẩm hoàn toàn tự động từ chỉ số Category Benchmark
     */
    private List<String> classifyProductTags(ProductItemDto p, CategoryBenchmark benchmark) {
        List<String> tags = new ArrayList<>();

        boolean isBestSeller = p.getSalesQuantity() > benchmark.getMedianQuantity();
        boolean isHighProfit = p.getProfitMargin() >= benchmark.getMedianProfitMargin();

        if (isBestSeller) {
            tags.add(BusinessTag.BEST_SELLER.name());
        }

        if (isHighProfit) {
            tags.add(BusinessTag.HIGH_PROFIT.name());
        }

        // Chỉ khuyến nghị giảm giá cho món thực sự ở phân khúc cao nhưng bán chậm.
        // Món HIGH_PROFIT vẫn được giữ giá theo hợp đồng nghiệp vụ ở trên.
        boolean isHighPriceSlowSales = !isBestSeller
                && !isHighProfit
                && p.getOriginalPrice() >= benchmark.getAveragePrice() * 1.20
                && p.getProfitMargin() >= 30.0;
        if (isHighPriceSlowSales) {
            tags.add(BusinessTag.SPECIAL_OFFER.name());
        }

        // REDUCED_PRIORITY = quantity < categoryMedianQuantity AND revenue < categoryMedianRevenue AND profitMargin < categoryMedianMargin
        boolean isReducedStrict = (p.getSalesQuantity() < benchmark.getMedianQuantity())
                && (p.getTotalRevenue() < benchmark.getMedianRevenue())
                && (p.getProfitMargin() < benchmark.getMedianProfitMargin());

        if (!isBestSeller && !isHighProfit && !isHighPriceSlowSales && isReducedStrict) {
            tags.add(BusinessTag.REDUCED_PRIORITY.name());
        }

        if (tags.isEmpty()) {
            tags.add(BusinessTag.CORE_PRODUCT.name());
        }

        return tags;
    }

    /**
     * Tính toán Benchmarks (Median & Average) cho từng Category trong sản phẩm
     */
    private Map<String, CategoryBenchmark> calculateCategoryBenchmarks(List<ProductItemDto> products) {
        Map<String, List<ProductItemDto>> grouped = products.stream().collect(Collectors.groupingBy(this::getCategoryName));
        Map<String, CategoryBenchmark> benchmarks = new HashMap<>();

        for (Map.Entry<String, List<ProductItemDto>> entry : grouped.entrySet()) {
            String catName = entry.getKey();
            List<ProductItemDto> catProds = entry.getValue();

            List<Double> quantities = catProds.stream().map(p -> (double) p.getSalesQuantity()).sorted().collect(Collectors.toList());
            List<Double> revenues = catProds.stream().map(ProductItemDto::getTotalRevenue).sorted().collect(Collectors.toList());
            List<Double> margins = catProds.stream().map(ProductItemDto::getProfitMargin).sorted().collect(Collectors.toList());

            double medianQty = computeMedian(quantities);
            double medianRev = computeMedian(revenues);
            double medianMargin = computeMedian(margins);

            double avgQty = quantities.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double avgRev = revenues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double avgMargin = margins.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double avgPrice = catProds.stream().mapToDouble(ProductItemDto::getOriginalPrice).average().orElse(0.0);

            CategoryBenchmark benchmark = CategoryBenchmark.builder()
                    .categoryName(catName)
                    .medianQuantity(medianQty)
                    .medianRevenue(medianRev)
                    .medianProfitMargin(medianMargin)
                    .averageQuantity(avgQty)
                    .averageRevenue(avgRev)
                    .averageMargin(avgMargin)
                    .averagePrice(avgPrice)
                    .build();

            benchmarks.put(catName, benchmark);
        }

        return benchmarks;
    }

    private List<String> getTopProductNamesFromText(String reportText, int limit) {
        if (reportText == null || reportText.isBlank()) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        String[] parts = reportText.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            int bracketIdx = trimmed.indexOf("(");
            if (bracketIdx > 0) {
                trimmed = trimmed.substring(0, bracketIdx).trim();
            }
            if (!trimmed.isBlank()) {
                names.add(trimmed);
            }
            if (names.size() >= limit) break;
        }
        return names;
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null || target.isBlank()) return false;
        return source.toLowerCase().contains(target.toLowerCase());
    }

    /**
     * Business Priority Rank cố định:
     * Priority 1: HIGH_PROFIT + BEST_SELLER
     * Priority 2: HIGH_PROFIT
     * Priority 3: BEST_SELLER
     * Priority 4: CORE_PRODUCT
     * Priority 5: REDUCED_PRIORITY
     */
    private int calculateBusinessPriority(List<String> tags) {
        boolean isBestSeller = tags.contains(BusinessTag.BEST_SELLER.name());
        boolean isHighProfit = tags.contains(BusinessTag.HIGH_PROFIT.name());
        boolean isSpecialOffer = tags.contains(BusinessTag.SPECIAL_OFFER.name());
        boolean isReduced = tags.contains(BusinessTag.REDUCED_PRIORITY.name());

        if (isHighProfit && isBestSeller) return 1;
        if (isSpecialOffer) return 2;
        if (isHighProfit) return 3;
        if (isBestSeller) return 4;
        if (isReduced) return 6;
        return 5; // CORE_PRODUCT
    }

    private Map<String, Double> calculateCategoryMedianQuantities(List<ProductItemDto> products) {
        Map<String, List<ProductItemDto>> grouped = products.stream().collect(Collectors.groupingBy(this::getCategoryName));
        Map<String, Double> medians = new HashMap<>();
        for (Map.Entry<String, List<ProductItemDto>> entry : grouped.entrySet()) {
            List<Double> list = entry.getValue().stream().map(p -> (double) p.getSalesQuantity()).sorted().collect(Collectors.toList());
            medians.put(entry.getKey(), computeMedian(list));
        }
        return medians;
    }

    private Map<String, Double> calculateCategoryMedianRevenues(List<ProductItemDto> products) {
        Map<String, List<ProductItemDto>> grouped = products.stream().collect(Collectors.groupingBy(this::getCategoryName));
        Map<String, Double> medians = new HashMap<>();
        for (Map.Entry<String, List<ProductItemDto>> entry : grouped.entrySet()) {
            List<Double> list = entry.getValue().stream().map(ProductItemDto::getTotalRevenue).sorted().collect(Collectors.toList());
            medians.put(entry.getKey(), computeMedian(list));
        }
        return medians;
    }

    private Map<String, Double> calculateCategoryMedianMargins(List<ProductItemDto> products) {
        Map<String, List<ProductItemDto>> grouped = products.stream().collect(Collectors.groupingBy(this::getCategoryName));
        Map<String, Double> medians = new HashMap<>();
        for (Map.Entry<String, List<ProductItemDto>> entry : grouped.entrySet()) {
            List<Double> list = entry.getValue().stream().map(ProductItemDto::getProfitMargin).sorted().collect(Collectors.toList());
            medians.put(entry.getKey(), computeMedian(list));
        }
        return medians;
    }

    private double calculateOverallMedianQuantity(List<ProductItemDto> products) {
        List<Double> list = products.stream().map(p -> (double) p.getSalesQuantity()).sorted().collect(Collectors.toList());
        return computeMedian(list);
    }

    private double calculateOverallMedianRevenue(List<ProductItemDto> products) {
        List<Double> list = products.stream().map(ProductItemDto::getTotalRevenue).sorted().collect(Collectors.toList());
        return computeMedian(list);
    }

    private double calculateOverallMedianMargin(List<ProductItemDto> products) {
        List<Double> list = products.stream().map(ProductItemDto::getProfitMargin).sorted().collect(Collectors.toList());
        return computeMedian(list);
    }

    private double computeMedian(List<Double> sortedList) {
        if (sortedList == null || sortedList.isEmpty()) return 0.0;
        int size = sortedList.size();
        if (size % 2 == 1) {
            return sortedList.get(size / 2);
        } else {
            return (sortedList.get(size / 2 - 1) + sortedList.get(size / 2)) / 2.0;
        }
    }

    /**
     * Sắp xếp Category Order:
     * 1. bestBusinessPriority ASC (MIN businessPriority của sản phẩm được giữ trong category)
     * 2. categoryProfit DESC
     * 3. categoryName ASC
     */
    private List<CategoryOrderDto> calculateCategoryOrder(
            List<ProductOptimizationDto> products,
            double totalRevenue,
            double totalProfit
    ) {
        Map<String, List<ProductOptimizationDto>> catMap = products.stream()
                .collect(Collectors.groupingBy(ProductOptimizationDto::getCategoryName, LinkedHashMap::new, Collectors.toList()));

        List<CategoryOrderDto> catList = new ArrayList<>();

        for (Map.Entry<String, List<ProductOptimizationDto>> entry : catMap.entrySet()) {
            String catName = entry.getKey();

            double catRev = entry.getValue().stream().mapToDouble(ProductOptimizationDto::getTotalRevenue).sum();
            double catProfit = entry.getValue().stream().mapToDouble(ProductOptimizationDto::getEstimatedProfit).sum();

            double revContrib = totalRevenue > 0 ? (catRev / totalRevenue) * 100.0 : 0.0;
            double profContrib = totalProfit > 0 ? (catProfit / totalProfit) * 100.0 : 0.0;

            double priorityScore = (profContrib * 0.60) + (revContrib * 0.40);

            CategoryOrderDto catDto = CategoryOrderDto.builder()
                    .categoryName(catName)
                    .categoryRevenue(round(catRev))
                    .categoryProfit(round(catProfit))
                    .priorityScore(round(priorityScore))
                    .build();

            catList.add(catDto);
        }

        // Sort: bestBusinessPriority ASC -> categoryProfit DESC -> categoryName ASC
        Map<String, Integer> catBestPriorityMap = products.stream()
                .collect(Collectors.groupingBy(
                        ProductOptimizationDto::getCategoryName,
                        Collectors.mapping(
                                p -> Boolean.TRUE.equals(p.getIncludeInFinalMenu()) ? p.getBusinessPriority() : 99,
                                Collectors.minBy(Integer::compare)
                        )
                )).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(99)));

        catList.sort((c1, c2) -> {
            int p1 = catBestPriorityMap.getOrDefault(c1.getCategoryName(), 99);
            int p2 = catBestPriorityMap.getOrDefault(c2.getCategoryName(), 99);
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
            int cProf = Double.compare(c2.getCategoryProfit(), c1.getCategoryProfit());
            if (cProf != 0) return cProf;
            return c1.getCategoryName().compareTo(c2.getCategoryName());
        });

        // Gán categoryOrder chính thức 1, 2, 3...
        for (int i = 0; i < catList.size(); i++) {
            catList.get(i).setCategoryOrder(i + 1);
            catList.get(i).setCategoryId("CAT" + String.format("%02d", i + 1));
        }

        return catList;
    }

    /**
     * Tạo Combo: Nghiệp vụ mới không tạo Combo, chuyển sang giảm giá kích cầu kèm ưu đãi quà tặng
     */
    private List<ComboRecommendationDto> generateComboRecommendations(List<ProductOptimizationDto> products) {
        return Collections.emptyList();
    }

    /**
     * Sắp xếp Thứ tự hiển thị Toàn cục & Trong Nhóm món
     */
    private void assignDisplayOrders(List<ProductOptimizationDto> products, List<CategoryOrderDto> categoryOrderList) {
        List<ProductOptimizationDto> included = products.stream()
                .filter(ProductOptimizationDto::getIncludeInFinalMenu)
                .collect(Collectors.toList());

        Comparator<ProductOptimizationDto> priorityComparator = (a, b) -> {
            int pA = a.getBusinessPriority() != null ? a.getBusinessPriority() : 4;
            int pB = b.getBusinessPriority() != null ? b.getBusinessPriority() : 4;
            if (pA != pB) {
                return Integer.compare(pA, pB);
            }
            int c1 = Double.compare(b.getEstimatedProfit(), a.getEstimatedProfit());
            if (c1 != 0) return c1;
            int c2 = Integer.compare(b.getSalesQuantity(), a.getSalesQuantity());
            if (c2 != 0) return c2;
            return a.getProductName().compareTo(b.getProductName());
        };

        // 1. Gán globalDisplayOrder toàn menu cho các món includeInFinalMenu
        included.sort(priorityComparator);
        for (int i = 0; i < included.size(); i++) {
            included.get(i).setGlobalDisplayOrder(i + 1);
        }

        // 2. Gán displayOrder trong từng Category
        Map<String, List<ProductOptimizationDto>> byCategory = included.stream()
                .collect(Collectors.groupingBy(ProductOptimizationDto::getCategoryName));

        for (CategoryOrderDto catDto : categoryOrderList) {
            List<ProductOptimizationDto> catProds = byCategory.getOrDefault(catDto.getCategoryName(), Collections.emptyList());
            catProds.sort(priorityComparator);
            for (int j = 0; j < catProds.size(); j++) {
                catProds.get(j).setDisplayOrder(j + 1);
            }
        }

        // Các món bị REMOVE / COMBO (includeInFinalMenu = false) -> displayOrder = null
        for (ProductOptimizationDto p : products) {
            if (!Boolean.TRUE.equals(p.getIncludeInFinalMenu())) {
                p.setDisplayOrder(null);
                p.setGlobalDisplayOrder(null);
            }
        }
    }

    private String getCategoryName(ProductItemDto p) {
        return (p.getCategory() != null && !p.getCategory().isBlank()) ? p.getCategory().trim() : "Khác";
    }

    private double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
