package com.smartmenu.service;

import com.smartmenu.dto.AnalysisReportDto;
import com.smartmenu.dto.CategoryRevenueDto;
import com.smartmenu.dto.ProductItemDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BusinessAnalysisService {

    public AnalysisReportDto analyzeBusinessData(

            List<ProductItemDto> products,

            List<CategoryRevenueDto> categoryRevenueList

    ) {

        if (products == null || products.isEmpty()) {

            throw new IllegalArgumentException(
                    "Không có dữ liệu sản phẩm để phân tích");

        }

        double totalRevenue = 0;

        double totalProfit = 0;

        int totalQuantity = 0;

        /*
         * Tính toán từng sản phẩm
         */

        for (ProductItemDto product : products) {
            double sellingPrice = product.getOriginalPrice() > 0 ? product.getOriginalPrice() : 0.0;
            double costPrice = product.getCostPrice();
            int quantity = product.getSalesQuantity();

            if (costPrice <= 0 && sellingPrice > 0) {
                // Nếu Excel chưa có giá vốn, tính ước lượng COGS F&B chuẩn (35% giá bán)
                costPrice = Math.round(sellingPrice * 0.35 * 10.0) / 10.0;
                product.setCostPrice(costPrice);
            }

            double revenue = sellingPrice * quantity;
            double profit = (sellingPrice - costPrice) * quantity;
            double margin = sellingPrice > 0 ? ((sellingPrice - costPrice) / sellingPrice) * 100.0 : 0.0;

            product.setTotalRevenue(revenue);
            product.setProfitMargin(margin);

            totalRevenue += revenue;
            totalProfit += profit;
            totalQuantity += quantity;

            log.info("[BUSINESS-METRIC] productId={} name={} sellingPrice={} costPrice={} quantity={} profit={} profitMargin={}",
                    product.getId(), product.getName(), sellingPrice, costPrice, quantity, profit, margin);
        }

        double averageMargin = 0;
        if (totalRevenue > 0) {
            averageMargin = (totalProfit / totalRevenue) * 100.0;
        }

        /*
         * BCG Matrix
         */

        classifyBCG(

                products,

                calculateAverageQuantity(products),

                averageMargin

        );

        /*
         * Revenue theo nhóm món
         */

        calculateCategoryRevenue(

                products,

                categoryRevenueList,

                totalRevenue

        );

        /*
         * Điểm tối ưu menu
         *
         * Không cố định
         *
         */

        int optimizationScore =

                calculateOptimizationScore(

                        products,

                        averageMargin

                );

        AnalysisReportDto report = AnalysisReportDto.builder()

                .totalRevenue(
                        round(totalRevenue))

                .totalItemsSold(
                        totalQuantity)

                .estimatedProfit(
                        round(totalProfit))

                .averageMargin(
                        round(averageMargin))

                .optimizationScore(
                        optimizationScore)

                .bestSellersText(
                        getTopSellingProducts(
                                products))

                .highProfitText(
                        getHighProfitProducts(
                                products))

                .lowPriorityText(
                        getLowPriorityProducts(
                                products))

                .heroDishesText(
                        getStarProducts(
                                products))

                .suggestedCombosText(
                        null)

                .build();

        log.info("[SECTION-2] highProfitProducts=[{}] | totalProfit={} | avgMargin={}% | score={}",
                report.getHighProfitText(), report.getEstimatedProfit(), report.getAverageMargin(), report.getOptimizationScore());

        return report;

    }

    /**
     *
     * Phân loại BCG
     *
     */

    private void classifyBCG(

            List<ProductItemDto> products,

            double avgQuantity,

            double avgMargin

    ) {

        for (ProductItemDto product : products) {

            boolean highSales =

                    product.getSalesQuantity() >= avgQuantity;

            boolean highProfit =

                    product.getProfitMargin() >= avgMargin;

            if (highSales && highProfit) {

                product.setBcgCategory(
                        "STAR");

            } else if (!highSales && highProfit) {

                product.setBcgCategory(
                        "PUZZLE");

            } else if (highSales && !highProfit) {

                product.setBcgCategory(
                        "CASH_COW");

            } else {

                product.setBcgCategory(
                        "DOG");

            }

        }

    }

    private double calculateAverageQuantity(

            List<ProductItemDto> products

    ) {

        return products.stream()

                .mapToInt(
                        ProductItemDto::getSalesQuantity)

                .average()

                .orElse(0);

    }

    private void calculateCategoryRevenue(

            List<ProductItemDto> products,

            List<CategoryRevenueDto> result,

            double totalRevenue

    ) {

        Map<String, List<ProductItemDto>> groups =

                products.stream()

                        .collect(

                                Collectors.groupingBy(

                                        p -> p.getCategory() == null
                                                ? "UNKNOWN"
                                                : p.getCategory()

                                )

                        );

        for (Map.Entry<String, List<ProductItemDto>> entry : groups.entrySet()) {

            double revenue =

                    entry.getValue()

                            .stream()

                            .mapToDouble(
                                    ProductItemDto::getTotalRevenue)

                            .sum();

            int quantity =

                    entry.getValue()

                            .stream()

                            .mapToInt(
                                    ProductItemDto::getSalesQuantity)

                            .sum();

            result.add(

                    CategoryRevenueDto.builder()

                            .name(
                                    entry.getKey())

                            .revenue(
                                    round(revenue))

                            .percentage(

                                    totalRevenue == 0

                                            ?

                                            0

                                            :

                                            round(
                                                    revenue
                                                            /
                                                            totalRevenue
                                                            *
                                                            100)

                            )

                            .itemQuantity(
                                    quantity)

                            .build()

            );

        }

    }

    private String getTopSellingProducts(

            List<ProductItemDto> products

    ) {

        return products.stream()

                .sorted(

                        Comparator.comparing(

                                ProductItemDto::getSalesQuantity

                        )
                                .reversed()

                )

                .limit(3)

                .map(

                        p -> p.getName()
                                +
                                " ("
                                +
                                p.getSalesQuantity()
                                +
                                " món)"

                )

                .collect(
                        Collectors.joining(", "));

    }

    private String getHighProfitProducts(

            List<ProductItemDto> products

    ) {

        return products.stream()

                .filter(
                        p -> p.getProfitMargin() > 0)

                .sorted(

                        Comparator.comparing(

                                ProductItemDto::getProfitMargin

                        )
                                .reversed()

                )

                .limit(3)

                .map(

                        p -> p.getName()
                                +
                                " ("
                                +
                                round(
                                        p.getProfitMargin())
                                +
                                "%)"

                )

                .collect(

                        Collectors.joining(", ")

                );

    }

    private String getLowPriorityProducts(

            List<ProductItemDto> products

    ) {

        List<ProductItemDto> dogList = products.stream()

                .filter(

                        p -> "DOG"
                                .equals(
                                        p.getBcgCategory())

                )
                .collect(Collectors.toList());

        if (dogList.isEmpty()) {
            // Nếu không có nhóm DOG, lấy 2 món có số lượng bán thấp nhất
            dogList = products.stream()
                    .sorted(Comparator.comparing(ProductItemDto::getSalesQuantity))
                    .limit(2)
                    .collect(Collectors.toList());
        }

        return dogList.stream()

                .map(

                        ProductItemDto::getName

                )

                .collect(

                        Collectors.joining(", ")

                );

    }

    private String getStarProducts(

            List<ProductItemDto> products

    ) {

        return products.stream()

                .filter(

                        p -> "STAR"
                                .equals(
                                        p.getBcgCategory())

                )

                .map(

                        ProductItemDto::getName

                )

                .collect(

                        Collectors.joining(", ")

                );

    }

    private int calculateOptimizationScore(

            List<ProductItemDto> products,

            double margin

    ) {

        long starCount =

                products.stream()

                        .filter(

                                p -> "STAR"
                                        .equals(
                                                p.getBcgCategory())

                        )

                        .count();

        double starRatio =

                ((double) starCount
                        /
                        products.size()

                )
                        *
                        100;

        double score =

                margin * 0.6

                        +

                        starRatio * 0.4;

        return (int)

        Math.min(

                100,

                Math.max(

                        0,

                        score

                )

        );

    }

    private double round(
            double value) {

        return Math.round(
                value * 10)
                /
                10.0;

    }

}