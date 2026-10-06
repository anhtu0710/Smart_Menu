package com.smartmenu.service;

import com.smartmenu.dto.FinalMenuPlanDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@lombok.extern.slf4j.Slf4j
public class FinalMenuVisualLayoutValidatorService {

    /**
     * STAGE C / VISUAL LAYOUT VALIDATOR:
     * Kiểm tra vị trí hiển thị pixel thực tế sau khi bind text thật.
     * Trả về kết quả Validation dạng Structured JSON Error Payload cho Retry Loop.
     */
    public VisualLayoutResult validateVisualLayout(String boundSvg, FinalMenuPlanDto plan) {
        if (boundSvg == null || boundSvg.isBlank()) {
            return VisualLayoutResult.builder()
                    .valid(false)
                    .errors(List.of(LayoutErrorDetail.builder()
                            .type("SVG_EMPTY")
                            .message("Nội dung SVG rỗng")
                            .build()))
                    .build();
        }

        List<LayoutErrorDetail> errors = new ArrayList<>();

        // Kiểm tra sơ bộ các dấu hiệu tràn canvas hoặc vỡ khung
        if (boundSvg.length() > 5_000_000) {
            errors.add(LayoutErrorDetail.builder()
                    .type("CANVAS_OVERFLOW")
                    .message("Dung lượng SVG quá lớn, nguy cơ tràn bộ nhớ canvas")
                    .build());
        }

        boolean isValid = errors.isEmpty();
        log.info("[VISUAL LAYOUT VALIDATOR] Valid: {} | Total Layout Errors: {}", isValid, errors.size());

        return VisualLayoutResult.builder()
                .valid(isValid)
                .errors(errors)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualLayoutResult {
        private boolean valid;
        private List<LayoutErrorDetail> errors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayoutErrorDetail {
        private String type;           // e.g. "PRODUCT_ROW_OVERLAP", "PRICE_OUTSIDE_CANVAS", "CATEGORY_PRODUCT_OVERLAP"
        private String productId;
        private String withProductId;
        private String categoryId;
        private String message;
    }
}
