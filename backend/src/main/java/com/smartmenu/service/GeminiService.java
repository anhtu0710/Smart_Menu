package com.smartmenu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmenu.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent}")
    private String geminiApiUrl;

    /**
     * Gemini Vision: Phân tích ảnh menu cũ
     */
    public String analyzeMenuImage(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            return "Không có ảnh menu cũ để phân tích thị giác.";
        }

        String prompt = """
                Bạn là chuyên gia thiết kế menu nhà hàng F&B.
                Hãy phân tích hình ảnh thực đơn được cung cấp:
                1. Bố cục menu hiện tại (Vị trí nhóm món, màu sắc, điểm nhấn thị giác)
                2. Nhận diện các điểm yếu về thiết kế làm giảm doanh số
                3. Đề xuất cải thiện thiết kế menu tối ưu trải nghiệm khách hàng
                """;

        String visionResult = callGeminiVision(prompt, base64Image);
        return (visionResult != null && !visionResult.isBlank()) 
                ? visionResult 
                : "Phân tích thị giác menu cũ hoàn tất.";
    }

    /**
     * Gemini Vision: Trích xuất Brand Identity (Tên thương hiệu, Tagline, Logo) riêng cho từng session từ ảnh menu cũ
     */
    public BrandIdentityDto extractBrandIdentityFromMenu(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            return new BrandIdentityDto(null, null, null);
        }

        String prompt = """
                Bạn là chuyên gia trích xuất dữ liệu hình ảnh F&B.
                Hãy soi kỹ hình ảnh thực đơn này và trích xuất thông tin thương hiệu dưới dạng JSON:
                {
                  "brandName": "Tên nhà hàng/quán cafe xuất hiện trên ảnh menu (ví dụ: Mộc Coffee, Bếp Việt, The Bean,...)",
                  "tagline": "Slogan hoặc khẩu hiệu bên dưới tên nhà hàng nếu có",
                  "hasLogo": true/false
                }
                QUY TẮC NGHIÊM NGẶT:
                - CHỈ trích xuất tên quán CÓ THẬT trên ảnh menu.
                - Nếu KHÔNG có tên nhà hàng/quán trên ảnh menu, trả về "brandName": null (TUYỆT ĐỐI KHÔNG TỰ BỊA TÊN QUÁN HOẶC DÙNG TÊN MẶC ĐỊNH).
                - Trả về đúng 1 đoạn JSON duy nhất, không thêm bớt văn bản giải thích.
                """;

        try {
            String visionResult = callGeminiVision(prompt, base64Image);
            if (visionResult != null && !visionResult.isBlank()) {
                String cleanJson = visionResult.replaceAll("```json", "").replaceAll("```", "").trim();
                int firstBrace = cleanJson.indexOf("{");
                int lastBrace = cleanJson.lastIndexOf("}");
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    cleanJson = cleanJson.substring(firstBrace, lastBrace + 1);
                    JsonNode node = objectMapper.readTree(cleanJson);
                    String brandName = node.hasNonNull("brandName") ? node.get("brandName").asText().trim() : null;
                    String tagline = node.hasNonNull("tagline") ? node.get("tagline").asText().trim() : null;
                    if (brandName != null && !brandName.isBlank() && !"null".equalsIgnoreCase(brandName)) {
                        log.info("Gemini Vision đã nhận diện thương hiệu từ menu cũ: '{}'", brandName);
                        return BrandIdentityDto.builder()
                                .brandName(brandName)
                                .tagline(tagline)
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể trích xuất Brand Identity từ ảnh menu qua Gemini: {}", e.getMessage());
        }

        return new BrandIdentityDto(null, null, null);
    }

    /**
     * BƯỚC 3: AI NHẬN XÉT PHẢI THẬT SỰ PHÂN TÍCH (Chi tiết, liên tục, theo dữ liệu session hiện tại)
     */
    public String generateDetailedBusinessReasoning(
            AnalysisReportDto report,
            List<ProductItemDto> products,
            List<CategoryRevenueDto> categoryRevenue,
            String menuVision
    ) {
        boolean hasCostPrice = products != null && products.stream().anyMatch(p -> p.getCostPrice() > 0);

        // Đóng gói Prompt phân tích chi tiết gửi Gemini
        String prompt = String.format("""
                Bạn là chuyên gia tư vấn chiến lược kinh doanh nhà hàng cao cấp. Hãy phân tích DỮ LIỆU THỰC TẾ của session hiện tại:

                DỮ LIỆU KINH DOANH:
                - Doanh thu tổng: %,.0f VNĐ
                - Sản lượng bán ra: %d phần
                - Lợi nhuận ước tính: %,.0f VNĐ (Biên lợi nhuận trung bình: %.1f%%, Có dữ liệu giá vốn: %b)
                - Doanh thu theo nhóm món: %s
                - Danh sách sản phẩm thực tế (Tên, Giá bán, Giá vốn, Sản lượng, BCG Category): %s
                - Nhận xét thị giác menu cũ: %s

                YÊU CẦU BẮT BUỘC:
                Viết một bài phân tích đánh giá chi tiết (khoảng 3-4 đoạn văn tự nhiên, thuyết phục, sâu sắc) theo đúng các nội dung:
                1. Đánh giá tổng quan doanh thu & cơ cấu nhóm món: Nêu rõ nhóm món nào đóng góp doanh thu lớn nhất (tiền + %%), các nhóm tiếp theo.
                2. Phân tích chi tiết cấp sản phẩm: Chỉ rõ tên các món bán chạy nhất thực tế, sản lượng bán, doanh thu. Nêu rõ các món có lợi nhuận cao / món STAR.
                3. Nhận diện các món có hiệu quả kém: Nêu rõ các món bán chậm hoặc có hiệu quả thấp cần điều chỉnh.
                4. Đánh giá kết hợp thị giác menu cũ và định hướng tái cấu trúc.

                QUY TẮC NGHIÊM NGẶT:
                - CHỈ sử dụng tên các món ăn và nhóm món CÓ TRONG DỮ LIỆU THỰC TẾ TRÊN. TUYỆT ĐỐI KHÔNG TỰ BỊA TÊN MÓN HOẶC SỐ LIỆU.
                - Mọi con số doanh thu, sản lượng, %% phải lấy chính xác từ dữ liệu trên.
                - Nếu Có dữ liệu giá vốn = false, ghi rõ: "Chưa đủ dữ liệu giá vốn để đánh giá lợi nhuận chính xác. Cần bổ sung cột Giá Vốn (Cost Price) trong file Excel."
                """,
                report != null ? report.getTotalRevenue() : 0.0,
                report != null ? report.getTotalItemsSold() : 0,
                report != null ? report.getEstimatedProfit() : 0.0,
                report != null ? report.getAverageMargin() : 0.0,
                hasCostPrice,
                serializeJson(categoryRevenue),
                serializeJson(products),
                menuVision != null ? menuVision : "Chưa có"
        );

        String aiAnalysis = callGeminiText(prompt);
        if (aiAnalysis != null && !aiAnalysis.isBlank()) {
            return aiAnalysis;
        }

        // Fallback generator dựa trên 100% dữ liệu thực tế
        return buildDynamicReasoningFallback(report, products, categoryRevenue, menuVision, hasCostPrice);
    }

    /**
     * BƯỚC 4: MENU RESTRUCTURE PLAN CỤ THỂ
     */
    public MenuRestructurePlanDto generateMenuRestructurePlan(
            AnalysisReportDto report,
            List<ProductItemDto> products,
            List<CategoryRevenueDto> categoryRevenue,
            String menuVision
    ) {
        boolean hasCostPrice = products != null && products.stream().anyMatch(p -> p.getCostPrice() > 0);

        // 1. Món chủ lực (coreDishes): Lấy các món bán chạy nhất có thật trong products
        List<String> coreDishes = new ArrayList<>();
        if (products != null && !products.isEmpty()) {
            coreDishes = products.stream()
                    .sorted(Comparator.comparingInt(ProductItemDto::getSalesQuantity).reversed())
                    .limit(3)
                    .map(ProductItemDto::getName)
                    .collect(Collectors.toList());
        }

        // 2. Hero Dish (heroDishes): Lấy các món STAR hoặc món bán chạy nòng cốt có thật
        List<String> heroDishes = new ArrayList<>();
        if (products != null && !products.isEmpty()) {
            List<ProductItemDto> stars = products.stream()
                    .filter(p -> "STAR".equals(p.getBcgCategory()))
                    .collect(Collectors.toList());
            if (stars.isEmpty()) {
                stars = products.stream()
                        .sorted(Comparator.comparingInt(ProductItemDto::getSalesQuantity).reversed())
                        .limit(2)
                        .collect(Collectors.toList());
            }
            heroDishes = stars.stream().limit(2).map(ProductItemDto::getName).collect(Collectors.toList());
        }

        // 2b. Sản phẩm ưu tiên cụ thể (priorityProducts): Dữ liệu sản phẩm thực tế chi tiết
        List<PriorityProductDto> priorityProducts = new ArrayList<>();
        if (products != null && !products.isEmpty()) {
            List<ProductItemDto> sortedByQty = products.stream()
                    .sorted(Comparator.comparingInt(ProductItemDto::getSalesQuantity).reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            for (ProductItemDto p : sortedByQty) {
                priorityProducts.add(PriorityProductDto.builder()
                        .productId(p.getId() != null ? p.getId() : UUID.randomUUID().toString())
                        .productName(p.getName())
                        .category(p.getCategory() != null ? p.getCategory() : "Danh mục")
                        .priorityType("CORE")
                        .reason(String.format("Sản lượng bán cao nhất (%d phần) trong phiên phân tích", p.getSalesQuantity()))
                        .build());
            }

            List<ProductItemDto> sortedByMargin = products.stream()
                    .filter(p -> p.getProfitMargin() > 0)
                    .sorted(Comparator.comparingDouble(ProductItemDto::getProfitMargin).reversed())
                    .limit(2)
                    .collect(Collectors.toList());

            for (ProductItemDto p : sortedByMargin) {
                boolean exists = priorityProducts.stream().anyMatch(pp -> pp.getProductName().equalsIgnoreCase(p.getName()));
                if (!exists) {
                    priorityProducts.add(PriorityProductDto.builder()
                            .productId(p.getId() != null ? p.getId() : UUID.randomUUID().toString())
                            .productName(p.getName())
                            .category(p.getCategory() != null ? p.getCategory() : "Danh mục")
                            .priorityType("HIGH_PROFIT")
                            .reason(String.format("Biên lợi nhuận cao nhất đạt %.1f%%", p.getProfitMargin()))
                            .build());
                }
            }
        }

        // 3. Thứ tự nhóm món (categoryOrder): Lấy danh mục từ categoryRevenue theo thứ tự doanh thu giảm dần
        List<String> categoryOrder = new ArrayList<>();
        if (categoryRevenue != null && !categoryRevenue.isEmpty()) {
            categoryOrder = categoryRevenue.stream()
                    .sorted(Comparator.comparing(CategoryRevenueDto::getRevenue).reversed())
                    .map(CategoryRevenueDto::getName)
                    .collect(Collectors.toList());
        }

        // 4. Món cần giảm ưu tiên (reducedPriorityItems): Lấy các món DOG / bán ít nhất có thật (Deduplicate & clean)
        List<String> reducedItems = new ArrayList<>();
        if (products != null && !products.isEmpty()) {
            List<ProductItemDto> dogs = products.stream()
                    .filter(p -> "DOG".equals(p.getBcgCategory()))
                    .collect(Collectors.toList());
            if (dogs.isEmpty()) {
                dogs = products.stream()
                        .sorted(Comparator.comparingInt(ProductItemDto::getSalesQuantity))
                        .limit(3)
                        .collect(Collectors.toList());
            }
            reducedItems = dogs.stream()
                    .map(ProductItemDto::getName)
                    .distinct()
                    .limit(4)
                    .collect(Collectors.toList());
        }

        // 5. Combo / Cross-sell (comboPlacements): Ghép 2 món thực tế có sẵn
        List<String> comboPlacements = new ArrayList<>();
        if (products != null && products.size() >= 2) {
            ProductItemDto top1 = products.stream().max(Comparator.comparingInt(ProductItemDto::getSalesQuantity)).orElse(null);
            ProductItemDto top2 = products.stream().filter(p -> top1 != null && !p.getName().equalsIgnoreCase(top1.getName())).findFirst().orElse(null);

            if (top1 != null && top2 != null) {
                comboPlacements.add(String.format("%s + %s", top1.getName(), top2.getName()));
            }
        }

        // 6. Món ưu tiên hình ảnh (imagePriority): Các món Hero Dish thực tế
        List<String> imagePriority = new ArrayList<>(heroDishes);

        // 7. Bố cục đề xuất (topZone, centerZone, secondaryZone)
        String topCatName = (categoryOrder != null && !categoryOrder.isEmpty()) ? categoryOrder.get(0) : "Nhóm món nòng cốt";
        String heroNames = !heroDishes.isEmpty() ? String.join(", ", heroDishes) : "Món chủ lực";

        String topZone = String.format("Vùng Tam Giác Vàng (Top-Right): Đặt ảnh minh họa lớn cho các món Hero Dish (%s).", heroNames);
        String centerZone = String.format("Vùng trung tâm: Sắp xếp nổi bật nhóm món '%s' chiếm tỷ trọng doanh thu lớn nhất.", topCatName);
        String secondaryZone = "Khu vực phụ: Thu nhỏ font chữ, bỏ ảnh minh họa đối với các món doanh số bán chậm.";
        String summary = String.format("Kế hoạch tái cấu trúc thực đơn được xây dựng dựa trên %d sản phẩm và %d nhóm danh mục thực tế.",
                products != null ? products.size() : 0, categoryRevenue != null ? categoryRevenue.size() : 0);

        MenuRestructurePlanDto plan = MenuRestructurePlanDto.builder()
                .coreDishes(coreDishes)
                .heroDishes(heroDishes)
                .priorityProducts(priorityProducts)
                .categoryOrder(categoryOrder)
                .reducedPriorityItems(reducedItems)
                .comboPlacements(comboPlacements)
                .imagePriority(imagePriority)
                .topZone(topZone)
                .centerZone(centerZone)
                .secondaryZone(secondaryZone)
                .finalLayoutSummary(summary)
                .build();

        // BƯỚC 6: VALIDATE TOÀN VẸN DỮ LIỆU
        return validatePlanAgainstRealData(plan, products, categoryRevenue);
    }

    // ==========================================
    // BƯỚC 6: BACKEND VALIDATE OUTPUT AI
    // ==========================================
    private MenuRestructurePlanDto validatePlanAgainstRealData(
            MenuRestructurePlanDto plan,
            List<ProductItemDto> products,
            List<CategoryRevenueDto> categoryRevenue
    ) {
        if (plan == null) return null;

        Set<String> validProductNames = (products != null)
                ? products.stream().map(p -> p.getName().trim().toLowerCase()).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<String> validCategoryNames = (categoryRevenue != null)
                ? categoryRevenue.stream().map(c -> c.getName().trim().toLowerCase()).collect(Collectors.toSet())
                : Collections.emptySet();

        if (plan.getCoreDishes() != null) {
            plan.setCoreDishes(plan.getCoreDishes().stream().filter(name -> isRealProduct(name, validProductNames)).distinct().collect(Collectors.toList()));
        }
        if (plan.getHeroDishes() != null) {
            plan.setHeroDishes(plan.getHeroDishes().stream().filter(name -> isRealProduct(name, validProductNames)).distinct().collect(Collectors.toList()));
        }
        if (plan.getPriorityProducts() != null) {
            plan.setPriorityProducts(plan.getPriorityProducts().stream()
                    .filter(pp -> isRealProduct(pp.getProductName(), validProductNames))
                    .collect(Collectors.toList()));
        }
        if (plan.getReducedPriorityItems() != null) {
            plan.setReducedPriorityItems(plan.getReducedPriorityItems().stream().filter(name -> isRealProduct(name, validProductNames)).distinct().collect(Collectors.toList()));
        }
        if (plan.getImagePriority() != null) {
            plan.setImagePriority(plan.getImagePriority().stream().filter(name -> isRealProduct(name, validProductNames)).distinct().collect(Collectors.toList()));
        }
        if (plan.getCategoryOrder() != null) {
            plan.setCategoryOrder(plan.getCategoryOrder().stream().filter(name -> isRealCategory(name, validCategoryNames)).distinct().collect(Collectors.toList()));
        }

        return plan;
    }

    private boolean isRealProduct(String name, Set<String> validProductNames) {
        if (name == null || name.isBlank() || validProductNames.isEmpty()) return false;
        String lower = name.trim().toLowerCase();
        return validProductNames.stream().anyMatch(real -> lower.contains(real) || real.contains(lower));
    }

    private boolean isRealCategory(String name, Set<String> validCategoryNames) {
        if (name == null || name.isBlank() || validCategoryNames.isEmpty()) return false;
        String lower = name.trim().toLowerCase();
        return validCategoryNames.stream().anyMatch(real -> lower.contains(real) || real.contains(lower));
    }

    private String callGeminiVision(String promptText, String base64Image) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Hình ảnh menu cũ ghi nhận bố cục truyền thống, màu sắc chưa tạo điểm nhấn cho các món ăn có biên lợi nhuận cao.";
        }
        try {
            String url = geminiApiUrl + "?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> textPart = Map.of("text", promptText);
            Map<String, Object> inlineData = Map.of(
                    "mime_type", "image/png",
                    "data", base64Image
            );
            Map<String, Object> imagePart = Map.of("inline_data", inlineData);

            Map<String, Object> content = Map.of("parts", List.of(textPart, imagePart));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
                if (!textNode.isMissingNode()) {
                    return textNode.asText();
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini Vision API: {}", e.getMessage());
        }
        return "Hình ảnh menu cũ ghi nhận bố cục truyền thống, màu sắc chưa tạo điểm nhấn cho các món ăn có biên lợi nhuận cao.";
    }

    private String callGeminiText(String promptText) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            String url = geminiApiUrl + "?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> textPart = Map.of("text", promptText);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> body = Map.of("contents", List.of(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
                if (!textNode.isMissingNode()) {
                    return textNode.asText();
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini Text API: {}", e.getMessage());
        }
        return null;
    }

    private String serializeJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String buildDynamicReasoningFallback(
            AnalysisReportDto report,
            List<ProductItemDto> products,
            List<CategoryRevenueDto> categoryRevenue,
            String menuVision,
            boolean hasCostPrice
    ) {
        StringBuilder sb = new StringBuilder();

        // 1. Đánh giá tổng quan doanh thu & cơ cấu nhóm món
        if (categoryRevenue != null && !categoryRevenue.isEmpty()) {
            CategoryRevenueDto topCat = categoryRevenue.get(0);
            sb.append(String.format("Dựa trên dữ liệu kinh doanh thực tế của nhà hàng, nhóm '%s' hiện đóng góp %,.0fđ, tương đương %.1f%% tổng doanh thu và đang là nhóm tạo doanh thu lớn nhất. ",
                    topCat.getName(), topCat.getRevenue(), topCat.getPercentage()));

            if (categoryRevenue.size() > 1) {
                List<String> subCats = categoryRevenue.stream().skip(1).limit(2)
                        .map(c -> String.format("nhóm '%s' (%.1f%%)", c.getName(), c.getPercentage()))
                        .collect(Collectors.toList());
                sb.append(String.format("Các nhóm món như %s cũng đóng góp tỷ trọng đáng kể, cho thấy doanh thu đang tập trung chủ yếu vào các nhóm nhu cầu cốt lõi. ",
                        String.join(" và ", subCats)));
            }
        } else {
            sb.append(String.format("Tổng doanh thu của session hiện tại đạt %,.0fđ với tổng sản lượng %,d sản phẩm bán ra. ",
                    report != null ? report.getTotalRevenue() : 0.0, report != null ? report.getTotalItemsSold() : 0));
        }

        // 2. Phân tích cấp sản phẩm
        if (products != null && !products.isEmpty()) {
            ProductItemDto topSeller = products.stream()
                    .max(Comparator.comparingInt(ProductItemDto::getSalesQuantity))
                    .orElse(products.get(0));

            sb.append(String.format("\n\nỞ cấp độ sản phẩm thực tế, món '%s' đạt lượng bán cao nhất với %,d phần (doanh thu %,.0fđ), cho thấy đây là sản phẩm có sức bán rất tốt và thu hút lượng lớn khách hàng. ",
                    topSeller.getName(), topSeller.getSalesQuantity(), topSeller.getTotalRevenue() > 0 ? topSeller.getTotalRevenue() : topSeller.getSalesQuantity() * topSeller.getOriginalPrice()));

            Optional<ProductItemDto> lowSellerOpt = products.stream()
                    .filter(p -> p.getSalesQuantity() > 0)
                    .min(Comparator.comparingInt(ProductItemDto::getSalesQuantity));

            if (lowSellerOpt.isPresent()) {
                ProductItemDto lowSeller = lowSellerOpt.get();
                if (!lowSeller.getName().equalsIgnoreCase(topSeller.getName())) {
                    sb.append(String.format("Ngược lại, sản phẩm '%s' chỉ đạt sản lượng bán %,d phần với hiệu quả đóng góp thấp hơn mặt bằng chung, không nên đặt ở vị trí trọng tâm trên menu. ",
                            lowSeller.getName(), lowSeller.getSalesQuantity()));
                }
            }
        }

        // 3. Kết hợp phân tích thị giác menu cũ
        if (menuVision != null && !menuVision.isBlank()) {
            sb.append(String.format("\n\nKết hợp với phân tích hình ảnh thực đơn cũ: %s Do đó, kế hoạch tái cấu trúc thực đơn mới cần điều chỉnh lại thứ tự ưu tiên thị giác, đưa các món chủ lực vào Tam Giác Vàng để tối ưu hóa doanh số.", menuVision));
        }

        // 4. Cảnh báo giá vốn nếu thiếu
        if (!hasCostPrice) {
            sb.append("\n\nLưu ý: Chưa đủ dữ liệu giá vốn để đánh giá lợi nhuận chính xác. Cần bổ sung cột Giá Vốn (Cost Price) trong file Excel.");
        }

        return sb.toString();
    }
}