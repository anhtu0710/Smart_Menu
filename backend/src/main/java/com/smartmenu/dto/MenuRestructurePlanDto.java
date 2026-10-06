package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuRestructurePlanDto {
    private List<String> coreDishes;           // Món chủ lực
    private List<String> heroDishes;           // Hero Dish
    private List<PriorityProductDto> priorityProducts; // Sản phẩm ưu tiên cụ thể
    private List<String> categoryOrder;        // Thứ tự nhóm món
    private List<String> reducedPriorityItems; // Món cần giảm ưu tiên
    private List<String> comboPlacements;      // Combo / Cross-sell dạng chuỗi
    private List<ComboPlacementDto> structuredCombos; // Combo cấu trúc chi tiết
    private List<PriceRecommendationDto> priceRecommendations; // Đề xuất giá thử nghiệm
    private List<String> imagePriority;        // Món ưu tiên hình ảnh
    private String topZone;                    // Vùng đầu
    private String centerZone;                 // Vùng trung tâm
    private String secondaryZone;              // Khu vực phụ
    private String finalLayoutSummary;         // Tóm tắt kế hoạch
}

