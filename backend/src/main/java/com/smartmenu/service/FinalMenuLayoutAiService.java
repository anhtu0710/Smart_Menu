package com.smartmenu.service;

import com.smartmenu.dto.ContentBlockDto;
import com.smartmenu.dto.ExclusionZoneDto;
import com.smartmenu.dto.LayoutJsonDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FinalMenuLayoutAiService {

    /**
     * TRUY XUẤT TEMPLATE LAYOUT MAP CỐ ĐỊNH CHO TỪNG ẢNH BACKGROUND THẬT
     * Tuyệt đối KHÔNG phụ thuộc vào tọa độ ngẫu nhiên từ AI.
     */
    public LayoutJsonDto generateLayoutJson(String templateId, int productCount, double avgNameLength) {
        String key = (templateId != null ? templateId : "COFFEE_MODERN_01").toUpperCase();

        LayoutJsonDto layout = buildTemplateLayoutMap(key, productCount);

        // Validation an toàn: Kiểm tra giao cắt giữa contentBlocks và exclusionZones
        validateLayoutAgainstExclusionZones(layout);

        return layout;
    }

    /**
     * Bounding Box Intersection Checker giữa Content Blocks và Exclusion Zones
     */
    public boolean validateLayoutAgainstExclusionZones(LayoutJsonDto layout) {
        if (layout.getExclusionZones() == null || layout.getExclusionZones().isEmpty()) {
            return true;
        }

        boolean isValid = true;
        for (ContentBlockDto block : layout.getContentBlocks()) {
            double blockRight = block.getLeftPercent() + block.getWidthPercent();
            double blockBottom = block.getTopPercent() + block.getHeightPercent();

            for (ExclusionZoneDto zone : layout.getExclusionZones()) {
                double zoneRight = zone.getLeftPercent() + zone.getWidthPercent();
                double zoneBottom = zone.getTopPercent() + zone.getHeightPercent();

                boolean intersects = !(
                        block.getLeftPercent() >= zoneRight ||
                        blockRight <= zone.getLeftPercent() ||
                        block.getTopPercent() >= zoneBottom ||
                        blockBottom <= zone.getTopPercent()
                );

                if (intersects) {
                    log.warn("[LAYOUT WARN] Content block '{}' (left:{}%, top:{}%) intersects exclusion zone '{}' in template '{}'",
                            block.getId(), block.getLeftPercent(), block.getTopPercent(), zone.getId(), layout.getTemplateId());
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    private LayoutJsonDto buildTemplateLayoutMap(String templateId, int productCount) {
        List<ContentBlockDto> blocks = new ArrayList<>();
        List<ExclusionZoneDto> exclusionZones = new ArrayList<>();

        int fontSize = 26;
        double lineHeight = 1.35;
        int columns = 2;
        String textColor = "#FFFFFF";
        String accentColor = "#F59E0B";
        String fontFamily = "SansSerif";

        switch (templateId) {
            case "COFFEE_LUXURY_01":
                textColor = "#1F2937";
                accentColor = "#BE185D";
                fontFamily = "Serif";
                // Vùng cấm: Viền pastel pink và các họa tiết hoa/ly/bánh ở 4 viền
                exclusionZones.add(new ExclusionZoneDto("pastel-border-top", 0, 0, 100, 12));
                exclusionZones.add(new ExclusionZoneDto("pastel-border-bottom", 0, 86, 100, 14));
                exclusionZones.add(new ExclusionZoneDto("pastel-border-left", 0, 0, 18, 100));
                exclusionZones.add(new ExclusionZoneDto("pastel-border-right", 82, 0, 18, 100));

                // Khoảng trắng trung tâm được chia thành 3 block theo chiều cao
                blocks.add(new ContentBlockDto("luxury-top", 20, 15, 60, 22, 10));
                blocks.add(new ContentBlockDto("luxury-mid", 20, 40, 60, 22, 10));
                blocks.add(new ContentBlockDto("luxury-bottom", 20, 65, 60, 20, 9));
                break;

            case "COFFEE_TRADITIONAL_01":
                textColor = "#292524";
                accentColor = "#78350F";
                fontFamily = "Serif";
                // Vùng cấm: Tách cà phê espresso ở dưới trái & Hạt cà phê ở trên phải
                exclusionZones.add(new ExclusionZoneDto("espresso-bottom-left", 0, 56, 48, 44));
                exclusionZones.add(new ExclusionZoneDto("beans-top-right", 65, 0, 35, 24));

                // Khoảng trắng trên khung giấy da mộc mạc
                blocks.add(new ContentBlockDto("traditional-upper-left", 6, 8, 54, 44, 12));
                blocks.add(new ContentBlockDto("traditional-center-right", 52, 26, 42, 32, 9));
                blocks.add(new ContentBlockDto("traditional-lower-right", 52, 62, 42, 32, 9));
                break;

            case "COFFEE_YOUTHFUL_01":
                textColor = "#0F172A";
                accentColor = "#059669";
                fontFamily = "SansSerif";
                // Vùng cấm: 3 ly đồ uống lớn (Matcha bên trái, Boba góc trên phải, Berry góc dưới phải)
                exclusionZones.add(new ExclusionZoneDto("matcha-glass-left", 0, 35, 35, 32));
                exclusionZones.add(new ExclusionZoneDto("boba-glass-top-right", 65, 5, 35, 35));
                exclusionZones.add(new ExclusionZoneDto("berry-glass-bottom-right", 65, 62, 35, 35));

                // Các khoảng trắng đan xen giữa 3 ly đồ uống
                blocks.add(new ContentBlockDto("youthful-top-center", 6, 8, 56, 24, 10));
                blocks.add(new ContentBlockDto("youthful-mid-right", 38, 38, 56, 24, 10));
                blocks.add(new ContentBlockDto("youthful-bottom-left", 6, 68, 56, 24, 10));
                break;

            case "COFFEE_MODERN_01":
            default:
                textColor = "#FFFFFF";
                accentColor = "#FBBF24";
                fontFamily = "SansSerif";
                // Vùng cấm: Bảng đồ uống smoothie bên trái
                exclusionZones.add(new ExclusionZoneDto("smoothie-left-panel", 0, 0, 42, 100));

                // Bảng Dark Navy bên phải chia thành 3 block nhỏ cân đối
                blocks.add(new ContentBlockDto("modern-top-right", 44, 8, 50, 26, 10));
                blocks.add(new ContentBlockDto("modern-mid-right", 44, 38, 50, 26, 10));
                blocks.add(new ContentBlockDto("modern-lower-right", 44, 68, 50, 26, 10));
                break;
        }

        // Tự động tinh chỉnh font size dựa trên tổng số món để đảm bảo fit 100% 1 trang
        if (productCount > 24) {
            fontSize = 19;
            lineHeight = 1.25;
        } else if (productCount > 14) {
            fontSize = 22;
            lineHeight = 1.3;
        }

        return LayoutJsonDto.builder()
                .templateId(templateId)
                .contentBlocks(blocks)
                .exclusionZones(exclusionZones)
                .fontSize(fontSize)
                .lineHeight(lineHeight)
                .columns(columns)
                .textColor(textColor)
                .accentColor(accentColor)
                .fontFamily(fontFamily)
                .build();
    }
}
