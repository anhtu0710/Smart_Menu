package com.smartmenu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmenu.dto.*;
import com.smartmenu.entity.GeneratedMenu;
import com.smartmenu.repository.GeneratedMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinalMenuCompositeService {

    private final GeneratedMenuRepository generatedMenuRepository;
    private final FinalMenuLayoutAiService finalMenuLayoutAiService;
    private final ObjectMapper objectMapper;

    // Cache kết quả sinh ảnh theo: sessionId + "_" + styleId + "_" + hash
    private final Map<String, FinalMenuResponseDto> menuCache = new ConcurrentHashMap<>();

    /**
     * BƯỚC 1 & 2: TRÍCH XUẤT VÀ KHÓA DỮ LIỆU SẢN PHẨM (LOCKED MENU DATA) TỪ VISUAL MENU BLUEPRINT
     */
    public List<LockedProductDto> extractAndFreezeLockedProducts(Long sessionId) {
        GeneratedMenu generatedMenu = generatedMenuRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Session " + sessionId));

        try {
            GeneratedMenuDataDto menuData = objectMapper.readValue(
                    generatedMenu.getGenerationData(),
                    GeneratedMenuDataDto.class
            );

            if (menuData.getBlueprint() == null || menuData.getBlueprint().getSections() == null) {
                return Collections.emptyList();
            }

            List<LockedProductDto> lockedList = new ArrayList<>();
            DecimalFormat formatter = getPriceFormatter();
            int order = 1;

            List<PriceRecommendationDto> priceRecs = menuData.getBlueprint().getPriceRecommendations();

            for (var section : menuData.getBlueprint().getSections()) {
                if (section.getDishes() == null) continue;
                for (var dish : section.getDishes()) {
                    if (dish.getName() == null || dish.getName().isBlank()) continue;

                    // Tìm đề xuất giá trong priceRecommendations
                    Double recPrice = null;
                    if (priceRecs != null) {
                        for (var rec : priceRecs) {
                            if (rec.getProductName() != null && rec.getProductName().trim().equalsIgnoreCase(dish.getName().trim())) {
                                recPrice = rec.getRecommendedPrice();
                                break;
                            }
                        }
                    }

                    // Quyết định giá hiển thị cuối cùng: Ưu tiên recommendedPrice nếu hợp lệ
                    Double finalPrice = (recPrice != null && recPrice > 0) ? recPrice : dish.getOriginalPrice();
                    if (finalPrice == null) finalPrice = 0.0;

                    String formattedPrice = formatter.format(finalPrice) + "đ";

                    lockedList.add(LockedProductDto.builder()
                            .id("prod_" + order)
                            .name(dish.getName().trim())
                            .price(finalPrice)
                            .formattedPrice(formattedPrice)
                            .order(order)
                            .categoryName(section.getCategoryName())
                            .originalPrice(dish.getOriginalPrice())
                            .recommendedPrice(recPrice)
                            .build());

                    order++;
                }
            }

            log.info("[DATA LOCK SUCCESS] Đã khóa {} sản phẩm từ VisualMenuBlueprint của Session {}",
                    lockedList.size(), sessionId);
            return lockedList;

        } catch (Exception e) {
            log.error("Lỗi parse VisualMenuBlueprint cho Session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Lỗi khóa dữ liệu Blueprint", e);
        }
    }

    /**
     * BƯỚC 3 & 4: DETERMINISTIC COMPOSITE RENDERER SỬ DỤNG JAVA GRAPHICS2D (HIGH DPI + REAL FONT MEASUREMENT)
     */
    public FinalMenuResponseDto generateFinalMenu(Long sessionId, String styleId, boolean debug) {
        String templateKey = (styleId != null ? styleId : "COFFEE_MODERN_01").toUpperCase();
        log.info("[FINAL-MENU] composite service entered | sessionId: {} | templateId: {} | debug: {}", sessionId, templateKey, debug);

        // 1. Tạo & khóa dữ liệu Locked Menu Data
        List<LockedProductDto> lockedProducts = extractAndFreezeLockedProducts(sessionId);
        log.info("[FINAL-MENU] blueprint loaded | sessionId: {} | locked products count: {}", sessionId, lockedProducts.size());
        if (lockedProducts.isEmpty()) {
            throw new IllegalStateException("Session " + sessionId + " không có dữ liệu món ăn để render");
        }

        // 2. Kiểm tra Cache (Nếu debug = true thì bỏ qua cache)
        String cacheKey = sessionId + "_" + templateKey + "_" + lockedProducts.size() + (debug ? "_debug" : "");
        if (!debug && menuCache.containsKey(cacheKey)) {
            log.info("[CACHE HIT] Trả về Final Menu Image từ Cache cho Session {} (Style: {})", sessionId, templateKey);
            return menuCache.get(cacheKey);
        }

        // 3. Tải ảnh Background mẫu và xác định kích thước/tỷ lệ canvas gốc
        File bgFile = findTemplateBgFile(templateKey);
        BufferedImage bgImage;
        try {
            bgImage = ImageIO.read(bgFile);
        } catch (IOException e) {
            log.error("Không thể đọc ảnh background template từ {}: {}", bgFile.getAbsolutePath(), e.getMessage());
            throw new RuntimeException("Không thể nạp ảnh template background", e);
        }

        int nativeWidth = bgImage.getWidth();
        int nativeHeight = bgImage.getHeight();
        double aspectRatio = (double) nativeWidth / nativeHeight;

        // Render ở độ phân giải cao High DPI (Scale 3x) giữ đúng tỷ lệ aspectRatio gốc
        int scaleFactor = 3;
        int renderWidth = nativeWidth * scaleFactor;
        int renderHeight = nativeHeight * scaleFactor;

        // 4. Lấy LayoutJsonDto từ Template Layout Map
        double avgNameLen = lockedProducts.stream().mapToInt(p -> p.getName().length()).average().orElse(15.0);
        LayoutJsonDto layoutJson = finalMenuLayoutAiService.generateLayoutJson(templateKey, lockedProducts.size(), avgNameLen);

        // Runtime Intersection Validation đối với Exclusion Zones
        boolean intersectsAny = !finalMenuLayoutAiService.validateLayoutAgainstExclusionZones(layoutJson);
        log.info("[LAYOUT] contentBlock intersects exclusionZone? -> {}", intersectsAny);

        // 5. Thuật toán Weighted Balanced Load Distribution
        List<BlockGroup> allocatedBlocks = allocateProductsToBlocks(lockedProducts, layoutJson.getContentBlocks());

        // Log thông tin từng block thực tế
        for (BlockGroup group : allocatedBlocks) {
            ContentBlockDto block = group.block;
            int blockX = (int) (block.getLeftPercent() / 100.0 * renderWidth);
            int blockY = (int) (block.getTopPercent() / 100.0 * renderHeight);
            int blockW = (int) (block.getWidthPercent() / 100.0 * renderWidth);
            int blockH = (int) (block.getHeightPercent() / 100.0 * renderHeight);
            log.info("[LAYOUT] blockId: {} | x: {} | y: {} | width: {} | height: {} | assignedProducts: {} | usedWeight: {}",
                    block.getId(), blockX, blockY, blockW, blockH, group.dishes.size(), group.usedWeight);
        }

        // 6. Tạo Canvas và cấu hình Graphics2D Anti-aliasing
        BufferedImage canvas = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvas.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Vẽ ảnh nền background gốc
        g2d.drawImage(bgImage, 0, 0, renderWidth, renderHeight, null);

        // Cấu hình Màu sắc
        Color textColor = parseColor(layoutJson.getTextColor(), Color.WHITE);
        Color accentColor = parseColor(layoutJson.getAccentColor(), new Color(245, 158, 11));

        // 7. THUẬT TOÁN ĐO TEXT THẬT & AUTO-FIT FONT SIZING (STEP-DOWN)
        int baseFontSize = (int) (layoutJson.getFontSize() * (renderWidth / 800.0));
        if (baseFontSize < 16) baseFontSize = 16;

        int minFontSize = 14;
        List<RenderManifestItemDto> manifest = new ArrayList<>();

        while (baseFontSize >= minFontSize) {
            manifest.clear();
            boolean fitsAllBlocks = true;

            Font nameFont = new Font("Dialog", Font.BOLD, baseFontSize);
            Font priceFont = new Font("Dialog", Font.BOLD, (int) (baseFontSize * 1.02));
            FontMetrics nameMetrics = g2d.getFontMetrics(nameFont);
            FontMetrics priceMetrics = g2d.getFontMetrics(priceFont);

            log.info("[LAYOUT] fontSize attempt = {}", baseFontSize);

            for (BlockGroup group : allocatedBlocks) {
                ContentBlockDto block = group.block;
                int blockX = (int) (block.getLeftPercent() / 100.0 * renderWidth);
                int blockY = (int) (block.getTopPercent() / 100.0 * renderHeight);
                int blockW = (int) (block.getWidthPercent() / 100.0 * renderWidth);
                int blockH = (int) (block.getHeightPercent() / 100.0 * renderHeight);

                int currentY = blockY + baseFontSize;
                int lineGap = (int) (baseFontSize * 0.35);

                for (LockedProductDto product : group.dishes) {
                    String priceStr = product.getFormattedPrice();
                    int priceW = priceMetrics.stringWidth(priceStr);
                    int maxNameW = blockW - priceW - 24;

                    if (maxNameW < 80) maxNameW = 80;

                    List<String> wrappedLines = wrapProductName(product.getName(), nameMetrics, maxNameW);
                    int itemHeight = wrappedLines.size() * (baseFontSize + lineGap);

                    if (currentY + itemHeight > blockY + blockH) {
                        fitsAllBlocks = false;
                        log.info("[LAYOUT] block '{}' requiredHeight > availableHeight (currentY: {} > max: {}), fits = false",
                                block.getId(), (currentY + itemHeight), (blockY + blockH));
                        break;
                    }

                    currentY += itemHeight + 6;
                }

                if (!fitsAllBlocks) break;
            }

            if (fitsAllBlocks || baseFontSize == minFontSize) {
                log.info("[LAYOUT] Font sizing resolved at fontSize = {}", baseFontSize);
                renderDishesToCanvas(g2d, allocatedBlocks, renderWidth, renderHeight, layoutJson, baseFontSize, textColor, accentColor, manifest);
                break;
            }

            baseFontSize -= 2;
            log.info("[LAYOUT] retry fontSize = {}", baseFontSize);
        }

        // 8. TẠO DEBUG OVERLAY TẠM THỜI TRÊN ẢNH NẾU DEBUG MODE HOẶC THEO YÊU CẦU
        if (debug) {
            drawDebugOverlay(g2d, allocatedBlocks, layoutJson.getExclusionZones(), renderWidth, renderHeight);
        }

        g2d.dispose();

        // 9. Chuyển đổi BufferedImage sang Base64 Data URI với Cache-Busting Timestamp
        long timestamp = System.currentTimeMillis();
        String base64Image = encodeImageToBase64(canvas);

        boolean isFullyValidated = (manifest.size() == lockedProducts.size());

        FinalMenuResponseDto response = FinalMenuResponseDto.builder()
                .finalImageUrl(base64Image)
                .templateId(templateKey)
                .totalDishesCount(manifest.size())
                .canvasWidth(nativeWidth)
                .canvasHeight(nativeHeight)
                .aspectRatio(aspectRatio)
                .manifest(manifest)
                .validated(isFullyValidated)
                .build();

        if (!debug) {
            menuCache.put(cacheKey, response);
        }

        log.info("[FINAL-MENU] final image generated | sessionId: {} | templateId: {} | dishes: {} | timestamp: {}",
                sessionId, templateKey, manifest.size(), timestamp);

        return response;
    }

    /**
     * VẼ DEBUG OVERLAY TRỰC TIẾP LÊN CANVAS
     * Khung Xanh = ContentBlocks (Dùng vùng nào để render text)
     * Khung Đỏ = ExclusionZones (Vùng cấm đè chữ: logo, ly nước, decor)
     */
    private void drawDebugOverlay(Graphics2D g2d, List<BlockGroup> allocatedBlocks, List<ExclusionZoneDto> exclusionZones, int width, int height) {
        log.info("[DEBUG OVERLAY] Drawing debug overlay onto canvas (Green = ContentBlocks, Red = ExclusionZones)");

        // 1. Vẽ Exclusion Zones (Khung Đỏ)
        if (exclusionZones != null) {
            for (ExclusionZoneDto zone : exclusionZones) {
                int zX = (int) (zone.getLeftPercent() / 100.0 * width);
                int zY = (int) (zone.getTopPercent() / 100.0 * height);
                int zW = (int) (zone.getWidthPercent() / 100.0 * width);
                int zH = (int) (zone.getHeightPercent() / 100.0 * height);

                g2d.setColor(new Color(239, 68, 68, 70)); // Transparent Red fill
                g2d.fillRect(zX, zY, zW, zH);

                g2d.setColor(new Color(220, 38, 38)); // Solid Red stroke
                g2d.setStroke(new BasicStroke(4.0f));
                g2d.drawRect(zX, zY, zW, zH);

                g2d.setFont(new Font("Dialog", Font.BOLD, 18));
                g2d.setColor(Color.RED);
                g2d.drawString("🚫 [EXCLUSION: " + zone.getId() + "]", zX + 10, zY + 25);
            }
        }

        // 2. Vẽ Content Blocks (Khung Xanh)
        if (allocatedBlocks != null) {
            for (BlockGroup group : allocatedBlocks) {
                ContentBlockDto block = group.block;
                int bX = (int) (block.getLeftPercent() / 100.0 * width);
                int bY = (int) (block.getTopPercent() / 100.0 * height);
                int bW = (int) (block.getWidthPercent() / 100.0 * width);
                int bH = (int) (block.getHeightPercent() / 100.0 * height);

                g2d.setColor(new Color(34, 197, 94, 60)); // Transparent Green fill
                g2d.fillRect(bX, bY, bW, bH);

                g2d.setColor(new Color(22, 163, 74)); // Solid Green stroke
                g2d.setStroke(new BasicStroke(4.0f));
                g2d.drawRect(bX, bY, bW, bH);

                g2d.setFont(new Font("Dialog", Font.BOLD, 18));
                g2d.setColor(new Color(22, 163, 74));
                g2d.drawString("✅ [BLOCK: " + block.getId() + " | Dishes: " + group.dishes.size() + "]", bX + 10, bY + 25);
            }
        }
    }


    /**
     * Thực hiện vẽ sản phẩm thực tế lên Canvas bằng Java Graphics2D
     */
    private void renderDishesToCanvas(Graphics2D g2d, List<BlockGroup> allocatedBlocks, int renderWidth, int renderHeight,
                                      LayoutJsonDto layoutJson, int fontSize, Color textColor, Color accentColor,
                                      List<RenderManifestItemDto> manifest) {
        Font nameFont = new Font("Dialog", Font.BOLD, fontSize);
        Font priceFont = new Font("Dialog", Font.BOLD, (int) (fontSize * 1.02));
        FontMetrics nameMetrics = g2d.getFontMetrics(nameFont);
        FontMetrics priceMetrics = g2d.getFontMetrics(priceFont);

        int lineGap = (int) (fontSize * 0.35);

        for (BlockGroup group : allocatedBlocks) {
            ContentBlockDto block = group.block;
            int blockX = (int) (block.getLeftPercent() / 100.0 * renderWidth);
            int blockY = (int) (block.getTopPercent() / 100.0 * renderHeight);
            int blockW = (int) (block.getWidthPercent() / 100.0 * renderWidth);

            int currentY = blockY + fontSize;

            for (LockedProductDto product : group.dishes) {
                String priceStr = product.getFormattedPrice();
                int priceW = priceMetrics.stringWidth(priceStr);
                int priceX = blockX + blockW - priceW;

                int maxNameW = blockW - priceW - 24;
                if (maxNameW < 80) maxNameW = 80;

                List<String> wrappedLines = wrapProductName(product.getName(), nameMetrics, maxNameW);

                // Vẽ Giá tiền (Align right ở dòng đầu tiên)
                g2d.setFont(priceFont);
                g2d.setColor(accentColor);
                g2d.drawString(priceStr, priceX, currentY);

                // Vẽ Tên món (Dòng 1 + Dòng 2 nếu có)
                g2d.setFont(nameFont);
                g2d.setColor(textColor);

                String line1 = wrappedLines.get(0);
                g2d.drawString(line1, blockX, currentY);

                // Vẽ Dotted Leader giữa dòng 1 của Tên và Giá
                int dotStartX = blockX + nameMetrics.stringWidth(line1) + 10;
                int dotEndX = priceX - 10;
                if (dotEndX > dotStartX) {
                    g2d.setColor(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 130));
                    drawDottedLine(g2d, dotStartX, currentY - 4, dotEndX, currentY - 4);
                }

                // Nếu tên sản phẩm có dòng 2
                if (wrappedLines.size() > 1) {
                    currentY += fontSize + lineGap;
                    g2d.setColor(textColor);
                    g2d.drawString(wrappedLines.get(1), blockX, currentY);
                }

                manifest.add(RenderManifestItemDto.builder()
                        .productId(product.getId())
                        .originalName(product.getName())
                        .renderedName(product.getName())
                        .expectedPrice(product.getPrice())
                        .renderedPrice(product.getPrice())
                        .blockId(block.getId())
                        .build());

                currentY += fontSize + lineGap + 8;
            }
        }
    }

    /**
     * Tự động chia dòng tên sản phẩm chính xác dựa trên đo đạc FontMetrics thực tế (Tối đa 2 dòng)
     */
    private List<String> wrapProductName(String name, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (metrics.stringWidth(name) <= maxWidth) {
            lines.add(name);
            return lines;
        }

        String[] words = name.split("\\s+");
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        boolean filledLine1 = false;

        for (String w : words) {
            if (!filledLine1) {
                String test = line1.length() == 0 ? w : line1 + " " + w;
                if (metrics.stringWidth(test) <= maxWidth) {
                    line1.append(line1.length() == 0 ? "" : " ").append(w);
                } else {
                    filledLine1 = true;
                    line2.append(w);
                }
            } else {
                line2.append(line2.length() == 0 ? "" : " ").append(w);
            }
        }

        lines.add(line1.toString());

        if (line2.length() > 0) {
            String l2 = line2.toString();
            if (metrics.stringWidth(l2) > maxWidth) {
                l2 = truncateWithEllipsis(l2, metrics, maxWidth);
            }
            lines.add(l2);
        }

        return lines;
    }

    private File findTemplateBgFile(String templateKey) {
        String filename;
        switch (templateKey) {
            case "COFFEE_LUXURY_01": filename = "coffee_luxury_bg.png"; break;
            case "COFFEE_TRADITIONAL_01": filename = "coffee_traditional_bg.png"; break;
            case "COFFEE_YOUTHFUL_01": filename = "coffee_youthful_bg.png"; break;
            case "COFFEE_MODERN_01":
            default: filename = "coffee_modern_bg.png"; break;
        }

        String[] candidatePaths = {
                "src/assets/templates/" + filename,
                "d:/Smart_Menu/src/assets/templates/" + filename,
                "public/assets/templates/" + filename,
                "target/classes/static/assets/templates/" + filename
        };

        for (String p : candidatePaths) {
            File f = new File(p);
            if (f.exists()) return f;
        }

        throw new IllegalArgumentException("Không tìm thấy ảnh background template: " + filename);
    }

    private List<BlockGroup> allocateProductsToBlocks(List<LockedProductDto> dishes, List<ContentBlockDto> blocks) {
        List<BlockGroup> groups = new ArrayList<>();
        for (ContentBlockDto b : blocks) {
            groups.add(new BlockGroup(b));
        }

        if (groups.isEmpty()) return groups;

        for (LockedProductDto dish : dishes) {
            double weight = dish.getName().length() > 20 ? 1.8 : 1.0;
            BlockGroup bestGroup = groups.get(0);
            double lowestRatio = bestGroup.usedWeight / (bestGroup.block.getCapacityWeight() > 0 ? bestGroup.block.getCapacityWeight() : 10.0);

            for (int i = 1; i < groups.size(); i++) {
                BlockGroup g = groups.get(i);
                double cap = g.block.getCapacityWeight() > 0 ? g.block.getCapacityWeight() : 10.0;
                double ratio = g.usedWeight / cap;
                if (ratio < lowestRatio) {
                    lowestRatio = ratio;
                    bestGroup = g;
                }
            }

            bestGroup.dishes.add(dish);
            bestGroup.usedWeight += weight;
        }

        return groups;
    }

    private void drawDottedLine(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        Stroke oldStroke = g2d.getStroke();
        float[] dash = {2.0f, 4.0f};
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1.0f, dash, 0.0f));
        g2d.drawLine(x1, y1, x2, y2);
        g2d.setStroke(oldStroke);
    }

    private String truncateWithEllipsis(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisW = metrics.stringWidth(ellipsis);
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (metrics.stringWidth(sb.toString() + c) + ellipsisW > maxWidth) {
                break;
            }
            sb.append(c);
        }
        return sb.toString() + ellipsis;
    }

    private Color parseColor(String hex, Color defaultColor) {
        if (hex == null || hex.isBlank()) return defaultColor;
        try {
            return Color.decode(hex.trim());
        } catch (Exception e) {
            return defaultColor;
        }
    }

    private DecimalFormat getPriceFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,###", symbols);
    }

    private String encodeImageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("Không thể mã hóa ảnh sang Base64: {}", e.getMessage());
            throw new RuntimeException("Lỗi mã hóa ảnh composite", e);
        }
    }

    private static class BlockGroup {
        ContentBlockDto block;
        List<LockedProductDto> dishes = new ArrayList<>();
        double usedWeight = 0;

        BlockGroup(ContentBlockDto block) {
            this.block = block;
        }
    }
}
