package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả Content Validation giữa FinalMenuPlanDto và Artwork do AI sinh ra.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkValidationResultDto {

    private String status; // "PASSED" hoặc "REJECTED"
    private boolean valid;
    private int matchedItemsCount;
    private int totalExpectedItemsCount;

    @Builder.Default
    private List<String> discrepancies = new ArrayList<>();

    private String validationMode; // "MANIFEST_MATCH" hoặc "OCR_FALLBACK"
}
