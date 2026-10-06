package com.smartmenu.service;

import com.smartmenu.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtworkContentValidatorService {

    /**
     * CONTENT VALIDATION (FINAL PRODUCTION ARCHITECTURE):
     * Backend acts as an uncompromising business guard.
     * Level 1: Structured Manifest Validation from AI output vs FinalMenuPlanDto.
     * Level 2: Fallback / Audit logging.
     *
     * If product names or prices differ, or required products are missing -> REJECT!
     */
    public ArtworkValidationResultDto validateArtworkContent(FinalMenuPlanDto expectedPlan, AiArtworkManifestDto manifest, String artworkImageUrl) {
        if (expectedPlan == null) {
            return ArtworkValidationResultDto.builder()
                    .status("REJECTED")
                    .valid(false)
                    .discrepancies(List.of("FinalMenuPlanDto is null - No business truth available"))
                    .build();
        }

        List<String> discrepancies = new ArrayList<>();
        int matchedCount = 0;
        int totalExpected = 0;

        // Level 1: Manifest Validation
        if (manifest != null && manifest.getCategories() != null && !manifest.getCategories().isEmpty()) {
            log.info("[CONTENT VALIDATION] Starting Level 1 Manifest Comparison against Business Truth");

            // Build map of manifest products for fast, normalized lookup
            Map<String, AiArtworkManifestDto.ProductManifest> manifestProductMap = new HashMap<>();
            for (AiArtworkManifestDto.CategoryManifest cat : manifest.getCategories()) {
                if (cat.getProducts() != null) {
                    for (AiArtworkManifestDto.ProductManifest prod : cat.getProducts()) {
                        if (prod.getName() != null) {
                            manifestProductMap.put(normalizeKey(prod.getName()), prod);
                        }
                    }
                }
            }

            // Compare each category and product from FinalMenuPlanDto
            if (expectedPlan.getCategories() != null) {
                for (CategoryPlanDto expCat : expectedPlan.getCategories()) {
                    if (expCat.getProducts() == null) continue;

                    for (ProductPlanDto expProd : expCat.getProducts()) {
                        if (Boolean.FALSE.equals(expProd.getIncludeInFinalMenu())) continue;

                        totalExpected++;
                        String normName = normalizeKey(expProd.getProductName());
                        AiArtworkManifestDto.ProductManifest matchedManifestProd = manifestProductMap.get(normName);

                        if (matchedManifestProd == null) {
                            // Try fuzzy match
                            matchedManifestProd = findFuzzyMatch(normName, manifestProductMap);
                        }

                        if (matchedManifestProd == null) {
                            String err = String.format("MISSING_PRODUCT: Sản phẩm '%s' (nhóm %s, giá %,.0fđ) không xuất hiện trên tác phẩm menu",
                                    expProd.getProductName(), expProd.getDisplayGroup(), expProd.getFinalDisplayPrice());
                            log.warn("[VALIDATION DISCREPANCY] {}", err);
                            discrepancies.add(err);
                        } else {
                            // Validate Price Accuracy
                            double expPrice = expProd.getFinalDisplayPrice() != null ? expProd.getFinalDisplayPrice() : 0.0;
                            double manifestPrice = matchedManifestProd.getPrice() != null ? matchedManifestProd.getPrice() : 0.0;

                            if (Math.abs(expPrice - manifestPrice) > 1.0) {
                                String err = String.format("PRICE_MISMATCH: Sản phẩm '%s' sai giá (Business Truth: %,.0fđ, Artwork: %,.0fđ)",
                                        expProd.getProductName(), expPrice, manifestPrice);
                                log.warn("[VALIDATION DISCREPANCY] {}", err);
                                discrepancies.add(err);
                            } else {
                                matchedCount++;
                            }
                        }
                    }
                }
            }

            boolean isValid = discrepancies.isEmpty();
            String status = isValid ? "PASSED" : "REJECTED";

            log.info("[CONTENT VALIDATION RESULT] status={} matched={}/{} discrepancies={}",
                    status, matchedCount, totalExpected, discrepancies.size());

            return ArtworkValidationResultDto.builder()
                    .status(status)
                    .valid(isValid)
                    .matchedItemsCount(matchedCount)
                    .totalExpectedItemsCount(totalExpected)
                    .discrepancies(discrepancies)
                    .validationMode("MANIFEST_MATCH")
                    .build();
        }

        // Level 2 Fallback: If no manifest provided by AI
        log.warn("[CONTENT VALIDATION] AI Manifest not provided. Falling back to basic integrity check");
        return ArtworkValidationResultDto.builder()
                .status("REJECTED")
                .valid(false)
                .discrepancies(List.of("AI did not produce a verifiable render manifest. Content cannot be guaranteed."))
                .validationMode("OCR_FALLBACK")
                .build();
    }

    private String normalizeKey(String str) {
        if (str == null) return "";
        return str.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private AiArtworkManifestDto.ProductManifest findFuzzyMatch(String targetKey, Map<String, AiArtworkManifestDto.ProductManifest> map) {
        for (Map.Entry<String, AiArtworkManifestDto.ProductManifest> entry : map.entrySet()) {
            String k = entry.getKey();
            if (k.contains(targetKey) || targetKey.contains(k)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
