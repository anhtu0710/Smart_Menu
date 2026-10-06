package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Render Manifest: DTO lưu kết quả danh mục và sản phẩm do AI sinh ra.
 * Dùng để đối chiếu 100% với Business Truth từ FinalMenuPlanDto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiArtworkManifestDto {

    @Builder.Default
    private List<CategoryManifest> categories = new ArrayList<>();

    @Builder.Default
    private List<ComboManifest> combos = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryManifest {

        private String name;

        @Builder.Default
        private List<ProductManifest> products = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductManifest {

        private String productId;

        private String name;

        private Double price;

        private String displayGroup;

        // Compatibility getter/setter for id
        public String getId() {
            return productId;
        }

        public void setId(String id) {
            this.productId = id;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboManifest {

        private String name;

        private Double price;
    }

    // Aliases for backward compatibility with existing tests
    public static class ManifestCategoryDto extends CategoryManifest {
        public static CategoryManifestBuilder builder() {
            return CategoryManifest.builder();
        }
    }

    public static class ManifestProductDto extends ProductManifest {
        public static ProductManifestBuilder builder() {
            return ProductManifest.builder();
        }
    }

    public static class ManifestComboDto extends ComboManifest {
        public static ComboManifestBuilder builder() {
            return ComboManifest.builder();
        }
    }
}
