package com.smartmenu.service;

import com.smartmenu.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class FinalMenuValidatorService {

    /**
     * STAGE A VALIDATION (PRE-BINDING):
     * Kiểm tra cấu trúc DOM SVG: Product IDs, Category IDs, Category Contract, Placement Groups.
     * TUYỆT ĐỐI KHÔNG DÙNG OCR.
     */
    public StageAResult validateStageA(String sanitizedSvg, FinalMenuPlanDto plan) {
        if (sanitizedSvg == null || plan == null || plan.getFinalMenuContent() == null) {
            return StageAResult.builder().valid(false).errorMessage("Dữ liệu đầu vào validation không hợp lệ").build();
        }

        try {
            Document doc = parseXmlSafely(sanitizedSvg);

            Set<String> expectedProductIds = new HashSet<>();
            Set<String> excludedProductIds = new HashSet<>(plan.getPlacementStrategy().getExcludedProductIds());
            Map<String, String> productCategoryMap = new HashMap<>();

            for (FinalMenuCategoryDto cat : plan.getFinalMenuContent().getCategories()) {
                for (FinalMenuProductDto p : cat.getProducts()) {
                    if (Boolean.TRUE.equals(p.getIncludeInFinalMenu())) {
                        expectedProductIds.add(p.getProductId());
                        productCategoryMap.put(p.getProductId(), cat.getCategoryId());
                    } else {
                        excludedProductIds.add(p.getProductId());
                    }
                }
            }

            Set<String> generatedProductIds = new HashSet<>();
            List<String> errors = new ArrayList<>();

            NodeList productNodes = doc.getElementsByTagName("*");
            for (int i = 0; i < productNodes.getLength(); i++) {
                Node node = productNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;

                    String prodId = el.getAttribute("data-product-id");
                    if (prodId != null && !prodId.isBlank()) {
                        prodId = prodId.trim();

                        if (generatedProductIds.contains(prodId)) {
                            errors.add("DUPLICATE_PRODUCT_ID: " + prodId);
                        }
                        generatedProductIds.add(prodId);

                        // Category Contract Validation: product.categoryId == DOM data-category-id
                        String domCatId = el.getAttribute("data-category-id");
                        String expectedCatId = productCategoryMap.get(prodId);
                        if (expectedCatId != null && domCatId != null && !domCatId.isBlank() && !expectedCatId.equalsIgnoreCase(domCatId.trim())) {
                            errors.add("WRONG_CATEGORY_MAPPING: Product " + prodId + " expected in category " + expectedCatId + " but placed in " + domCatId);
                        }

                        // Excluded Product Check
                        if (excludedProductIds.contains(prodId)) {
                            errors.add("EXCLUDED_PRODUCT_APPEARED: Product " + prodId + " is excluded but appeared in SVG");
                        }
                    }
                }
            }

            // Kiểm tra thiếu hoặc dư Product ID
            Set<String> missingIds = new HashSet<>(expectedProductIds);
            missingIds.removeAll(generatedProductIds);

            Set<String> unknownIds = new HashSet<>(generatedProductIds);
            unknownIds.removeAll(expectedProductIds);

            if (!missingIds.isEmpty()) {
                errors.add("MISSING_PRODUCT_IDS: " + missingIds);
            }
            if (!unknownIds.isEmpty()) {
                errors.add("UNKNOWN_PRODUCT_IDS: " + unknownIds);
            }

            boolean isValid = errors.isEmpty();
            log.info("[STAGE A VALIDATION] Valid: {} | Expected: {} | Generated: {} | Errors: {}",
                    isValid, expectedProductIds.size(), generatedProductIds.size(), errors);

            return StageAResult.builder()
                    .valid(isValid)
                    .missingProductIds(new ArrayList<>(missingIds))
                    .unknownProductIds(new ArrayList<>(unknownIds))
                    .errors(errors)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi Stage A Validation: {}", e.getMessage());
            return StageAResult.builder().valid(false).errorMessage("Lỗi parse SVG XML: " + e.getMessage()).build();
        }
    }

    /**
     * STAGE B VALIDATION (POST-BINDING):
     * Kiểm tra không còn {{PLACEHOLDER}} và cú pháp XML hợp lệ.
     */
    public boolean validateStageB(String boundSvg) {
        if (boundSvg == null || boundSvg.isBlank()) return false;
        if (boundSvg.contains("{{") || boundSvg.contains("}}")) {
            log.error("[STAGE B VALIDATION FAIL] SVG chứa placeholder chưa bind: {}", boundSvg.substring(0, Math.min(boundSvg.length(), 200)));
            return false;
        }

        try {
            parseXmlSafely(boundSvg);
            log.info("[STAGE B VALIDATION SUCCESS] SVG XML hợp lệ và 100% placeholders đã được tiêm chữ");
            return true;
        } catch (Exception e) {
            log.error("[STAGE B VALIDATION FAIL] Lỗi XML DOM sau khi bind: {}", e.getMessage());
            return false;
        }
    }

    private Document parseXmlSafely(String xmlContent) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    @lombok.Data
    @lombok.Builder
    public static class StageAResult {
        private boolean valid;
        private String errorMessage;
        private List<String> missingProductIds;
        private List<String> unknownProductIds;
        private List<String> errors;
    }
}
