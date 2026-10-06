package com.smartmenu.service;

import com.smartmenu.dto.CategoryPlanDto;
import com.smartmenu.dto.FinalMenuPlanDto;
import com.smartmenu.dto.MenuStyleConfigDto;
import com.smartmenu.dto.ProductPlanDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class MenuCompositeArtworkRenderer {

    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 1131;

    /**
     * Renders a complete, commercial-grade menu poster combining:
     * 1. High-end food/beverage photography from the style reference asset.
     * 2. Real business data (categories, dishes, prices) locked in FinalMenuPlanDto.
     * 3. 100% faithful layout, typography and aesthetic mimicking each style reference.
     */
    public byte[] renderCompositeArtwork(
            byte[] referenceImageBytes,
            FinalMenuPlanDto plan,
            String styleId,
            MenuStyleConfigDto styleConfig) {

        BufferedImage canvas = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = canvas.createGraphics();

        try {
            // Highest quality rendering and sub-pixel antialiasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            String style = styleId != null ? styleId.toUpperCase(Locale.ROOT) : "HIEN_DAI";

            // If SANG_TRONG, preserve 100% original reference composition (1102x762), only inpaint the business text
            if ("SANG_TRONG".equals(style) || "LUXURY".equals(style) || "COFFEE_LUXURY_01".equals(style)) {
                byte[] luxuryResult = renderLuxuryOriginalStyle(referenceImageBytes, plan, styleConfig);
                if (luxuryResult != null && luxuryResult.length > 0) {
                    g2d.dispose();
                    return luxuryResult;
                }
            }

            // If TRUYEN_THONG, preserve 100% original reference composition (682x1024), only inpaint the business items
            if ("TRUYEN_THONG".equals(style) || "VINTAGE".equals(style) || "COFFEE_TRADITIONAL_01".equals(style)) {
                byte[] vintageResult = renderVintageBrewOriginalStyle(referenceImageBytes, plan, styleConfig);
                if (vintageResult != null && vintageResult.length > 0) {
                    g2d.dispose();
                    return vintageResult;
                }
            }

            // If TRE_TRUNG, preserve 100% original reference composition (1254x1254), inpaint clean 100% all old text, and map business data
            if ("TRE_TRUNG".equals(style) || "CUTE".equals(style) || "COFFEE_YOUTHFUL_01".equals(style)) {
                byte[] youthfulResult = renderYouthfulOriginalStyle(referenceImageBytes, plan, styleConfig);
                if (youthfulResult != null && youthfulResult.length > 0) {
                    g2d.dispose();
                    return youthfulResult;
                }
            }

            switch (style) {
                default ->
                        renderModernStyle(g2d, referenceImageBytes, plan, styleConfig);
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", baos);
            byte[] result = baos.toByteArray();
            log.info("[COMPOSITE RENDERER] Rendered high-res menu artwork: {}x{} ({} bytes) for style {}",
                    CANVAS_WIDTH, CANVAS_HEIGHT, result.length, style);
            return result;
        } catch (Exception e) {
            log.error("[COMPOSITE RENDERER] Error rendering composite menu artwork: {}", e.getMessage(), e);
            g2d.dispose();
            throw new IllegalStateException("COMPOSITE_MENU_RENDER_FAILED", e);
        }
    }

    // =========================================================================
    // 4. STYLE: TRE_TRUNG (YOUTHFUL CUTE CAFE - 100% PRESERVED REFERENCE LAYOUT)
    // Preserves 100% authentic reference artwork (1024x1024): Left brand column,
    // giant iced latte photo, 4 decorative beverage illustrations, and bottom Topping bar.
    // Inpaints clean 100% all old text, and renders real business dishes in the 4 categories.
    // =========================================================================
    private byte[] renderYouthfulOriginalStyle(
            byte[] referenceImageBytes,
            FinalMenuPlanDto plan,
            MenuStyleConfigDto styleConfig) {

        try {
            BufferedImage canvas = null;
            // Ưu tiên load template_clean.png đã được làm sạch 100% text cũ và giữ trọn hình minh họa
            try (InputStream cleanStream = getClass().getResourceAsStream("/menu-style/TRE_TRUNG/template_clean.png")) {
                if (cleanStream != null) {
                    canvas = ImageIO.read(cleanStream);
                }
            } catch (Exception ignored) {}

            // Fallback load reference bytes nếu không có template_clean
            if (canvas == null && referenceImageBytes != null && referenceImageBytes.length > 0) {
                canvas = ImageIO.read(new ByteArrayInputStream(referenceImageBytes));
                if (canvas != null) {
                    inpaintYouthfulBox(canvas, 308, 88, 330, 270, new Color(251, 233, 226), 10);
                    inpaintYouthfulBox(canvas, 668, 88, 326, 142, new Color(236, 238, 227), 10);
                    inpaintYouthfulBox(canvas, 308, 482, 330, 148, new Color(250, 238, 226), 10);
                    inpaintYouthfulBox(canvas, 668, 476, 326, 74, new Color(227, 232, 242), 10);
                }
            }

            if (canvas == null) return null;

            Graphics2D g2d = canvas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // Map dữ liệu thực tế từ plan.getCategories() vào 4 ô danh mục đã được làm sạch
            List<CategoryPlanDto> categories = plan != null && plan.getCategories() != null
                    ? plan.getCategories()
                    : List.of();

            if (!categories.isEmpty()) {
                if (categories.size() >= 4) {
                    renderYouthfulItemsInBox(g2d, getIncludedProducts(categories.get(0)), 318, 122, 312, 40, 6, new Color(190, 24, 93));
                    renderYouthfulItemsInBox(g2d, getIncludedProducts(categories.get(1)), 678, 122, 308, 40, 4, new Color(47, 133, 90));
                    renderYouthfulItemsInBox(g2d, getIncludedProducts(categories.get(2)), 318, 515, 312, 40, 4, new Color(217, 119, 6));
                    renderYouthfulItemsInBox(g2d, getIncludedProducts(categories.get(3)), 678, 510, 308, 40, 2, new Color(37, 99, 235));
                } else if (categories.size() == 3) {
                    renderYouthfulItemsInBox(g2d, getIncludedProducts(categories.get(0)), 318, 122, 312, 40, 6, new Color(190, 24, 93));
                    renderYouthfulItemsInBox(g2d, getIncludedProducts(categories.get(1)), 678, 122, 308, 40, 4, new Color(47, 133, 90));
                    renderYouthfulItemsInBox(g2d, getIncludedProducts(categories.get(2)), 318, 515, 312, 40, 4, new Color(217, 119, 6));
                } else if (categories.size() == 2) {
                    List<ProductPlanDto> p1 = getIncludedProducts(categories.get(0));
                    List<ProductPlanDto> p2 = getIncludedProducts(categories.get(1));
                    int mid1 = Math.min(p1.size(), 6);
                    renderYouthfulItemsInBox(g2d, p1.subList(0, mid1), 318, 122, 312, 40, 6, new Color(190, 24, 93));
                    if (p1.size() > mid1) {
                        renderYouthfulItemsInBox(g2d, p1.subList(mid1, p1.size()), 318, 515, 312, 40, 4, new Color(217, 119, 6));
                    }
                    int mid2 = Math.min(p2.size(), 3);
                    renderYouthfulItemsInBox(g2d, p2.subList(0, mid2), 678, 122, 308, 40, 3, new Color(47, 133, 90));
                    if (p2.size() > mid2) {
                        renderYouthfulItemsInBox(g2d, p2.subList(mid2, p2.size()), 678, 510, 308, 40, 2, new Color(37, 99, 235));
                    }
                } else {
                    List<ProductPlanDto> p = getIncludedProducts(categories.get(0));
                    int s0 = Math.min(p.size(), 6);
                    renderYouthfulItemsInBox(g2d, p.subList(0, s0), 318, 122, 312, 40, 6, new Color(190, 24, 93));
                    int s1 = Math.min(p.size(), s0 + 3);
                    if (p.size() > s0) {
                        renderYouthfulItemsInBox(g2d, p.subList(s0, s1), 678, 122, 308, 40, 3, new Color(47, 133, 90));
                    }
                    int s2 = Math.min(p.size(), s1 + 3);
                    if (p.size() > s1) {
                        renderYouthfulItemsInBox(g2d, p.subList(s1, s2), 318, 515, 312, 40, 3, new Color(217, 119, 6));
                    }
                    if (p.size() > s2) {
                        renderYouthfulItemsInBox(g2d, p.subList(s2, p.size()), 678, 510, 308, 40, 2, new Color(37, 99, 235));
                    }
                }
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", baos);
            byte[] result = baos.toByteArray();
            log.info("[COMPOSITE RENDERER] Rendered Youthful clean menu artwork: {}x{} ({} bytes)",
                    canvas.getWidth(), canvas.getHeight(), result.length);
            return result;
        } catch (Exception e) {
            log.error("[COMPOSITE RENDERER] Error rendering youthful composite menu artwork: {}", e.getMessage(), e);
            return null;
        }
    }

    private void inpaintYouthfulBox(BufferedImage canvas, int x, int y, int w, int h, Color color, int radius) {
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillRoundRect(x, y, w, h, radius * 2, radius * 2);
        g.dispose();
    }

    private void renderYouthfulItemsInBox(
            Graphics2D g2d,
            List<ProductPlanDto> items,
            int leftX, int startY, int width,
            int lineHeight, int maxItems,
            Color themeColor) {

        if (items == null || items.isEmpty()) return;
        int count = Math.min(items.size(), maxItems);

        Font fontItem = new Font("Segoe UI", Font.BOLD, 11);
        Font fontGift = new Font("Segoe UI", Font.ITALIC, 9);
        Font fontPrice = new Font("Segoe UI", Font.BOLD, 11);
        int curY = startY;

        for (int i = 0; i < count; i++) {
            ProductPlanDto prod = items.get(i);
            if (prod == null) continue;

            String name = prod.getProductName() != null ? prod.getProductName().trim() : "";
            if (name.isBlank()) continue;

            String price = formatPrice(prod.getFinalDisplayPrice());

            String rawGift = prod.getPromoGift();
            boolean hasGift = rawGift != null && !rawGift.isBlank();
            String giftText = "";
            if (hasGift) {
                String cleanGift = rawGift.replace("Tặng ", "").replace("tặng ", "").trim();
                giftText = "(Ưu đãi: " + cleanGift + ")";
            }

            // Icon chấm tròn nhận diện (HERO đỏ, Promo cam, thường xám)
            if ("HERO".equalsIgnoreCase(prod.getDisplayGroup())) {
                g2d.setColor(new Color(225, 29, 72)); // Đỏ nổi bật
                g2d.fillOval(leftX + 2, curY - 8, 6, 6);
            } else if (hasGift) {
                g2d.setColor(new Color(217, 119, 6)); // Cam ưu đãi / quà tặng
                g2d.fillOval(leftX + 2, curY - 8, 6, 6);
            } else {
                g2d.setColor(new Color(180, 190, 205)); // Xám trang nhã
                g2d.fillOval(leftX + 3, curY - 7, 4, 4);
            }

            // Đo độ rộng giá tiền
            FontMetrics fmPrice = g2d.getFontMetrics(fontPrice);
            int priceW = fmPrice.stringWidth(price);
            int priceX = leftX + width - priceW;

            // Dynamic Font Scaling nếu tên món quá dài
            Font currentFont = fontItem;
            FontMetrics fmItem = g2d.getFontMetrics(currentFont);
            int maxNameW = width - priceW - 22;

            if (fmItem.stringWidth(name) > maxNameW) {
                float targetSize = 11.0f;
                while (targetSize > 8.5f && fmItem.stringWidth(name) > maxNameW) {
                    targetSize -= 0.5f;
                    currentFont = fontItem.deriveFont(targetSize);
                    fmItem = g2d.getFontMetrics(currentFont);
                }
            }

            // Vẽ tên món đầy đủ ở dòng 1
            g2d.setFont(currentFont);
            g2d.setColor(new Color(35, 30, 28));
            g2d.drawString(name, leftX + 14, curY);

            // Vẽ các chấm nối leader dots
            int nameW = fmItem.stringWidth(name);
            int dotStart = leftX + 18 + nameW;
            int dotEnd = priceX - 6;
            if (dotEnd > dotStart) {
                g2d.setColor(new Color(195, 200, 210));
                for (int dx = dotStart; dx < dotEnd; dx += 6) {
                    g2d.fillRect(dx, curY - 3, 2, 2);
                }
            }

            // Vẽ giá tiền
            g2d.setFont(fontPrice);
            g2d.setColor(themeColor);
            g2d.drawString(price, priceX, curY);

            // Dòng 2: Hiển thị rõ ràng nghiệp vụ combo / ưu đãi đi kèm
            if (hasGift && !giftText.isBlank()) {
                g2d.setFont(fontGift);
                g2d.setColor(new Color(180, 83, 9)); // Màu cam hổ phách sang trọng
                g2d.drawString(giftText, leftX + 18, curY + 13);
            }

            curY += lineHeight;
        }
    }

    // =========================================================================
    // 1. STYLE: HIEN_DAI (MODERN BLACK ESPRESSO CAFE)
    // Matches reference: Deep black, geometric diamond emblem, studio drinks at bottom,
    // crisp white typography, amber prices, cyan glowing dividers.
    // =========================================================================
    private void renderModernStyle(
            Graphics2D g2d,
            byte[] referenceImageBytes,
            FinalMenuPlanDto plan,
            MenuStyleConfigDto styleConfig) {

        // 1. Deep black background
        g2d.setColor(new Color(10, 10, 12));
        g2d.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // 2. Real commercial beverage photography at bottom
        drawReferenceVisual(g2d, referenceImageBytes, 0.65, 655, CANVAS_HEIGHT - 655, new Color(10, 10, 12));

        // 3. Geometric emblem at top center
        drawGeometricEmblem(g2d, 400, 75, new Color(240, 240, 245));

        // 4. Header: Brand / Concept Title
        String brandTitle = resolveBrandTitle(plan, "CURATED COFFEE");
        g2d.setFont(new Font("SansSerif", Font.BOLD, 26));
        g2d.setColor(new Color(255, 255, 255));
        drawCenteredString(g2d, brandTitle, 400, 135);

        // Subtitle line
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(new Color(160, 165, 180));
        drawCenteredString(g2d, "ARTISAN COFFEE & BEVERAGE MENU", 400, 160);

        // Thin accent divider
        g2d.setColor(new Color(56, 189, 248, 140)); // Cyan glowing accent
        g2d.fillRect(320, 178, 160, 2);

        // 5. Draw Categories & Products in 2 balanced columns
        List<CategoryPlanDto> categories = plan != null && plan.getCategories() != null
                ? plan.getCategories()
                : List.of();

        if (categories.isEmpty()) return;

        int col1X = 70;
        int col1Width = 310;
        int col2X = 420;
        int col2Width = 310;

        Color headerColor = new Color(56, 189, 248); // Cyan
        Color itemColor = Color.WHITE;
        Color priceColor = new Color(245, 158, 11); // Amber gold

        renderCategoryGrid(g2d, categories, col1X, col2X, col1Width, col2Width, 210, 415,
                headerColor, itemColor, priceColor, "SansSerif", false);
    }

    // =========================================================================
    // =========================================================================
    // 2. STYLE: SANG_TRONG (ROYAL LUXURY & FINE DINING CAFE - 100% PRESERVED REFERENCE)
    // Preserves 100% of the reference design (1102x762), background, calligraphy header,
    // table marble ledge, and full studio cocktail photography.
    // Only seamlessly replaces the text block with real business dishes & prices.
    // =========================================================================
    private byte[] renderLuxuryOriginalStyle(
            byte[] referenceImageBytes,
            FinalMenuPlanDto plan,
            MenuStyleConfigDto styleConfig) {

        if (referenceImageBytes == null || referenceImageBytes.length == 0) {
            return null;
        }

        try {
            BufferedImage canvas = ImageIO.read(new ByteArrayInputStream(referenceImageBytes));
            if (canvas == null) return null;

            Graphics2D g2d = canvas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // 1. Seamless inpaint of the old category text region (X: 520..1085, Y: 22..348)
            int patchX = 520;
            int patchY = 22;
            int patchW = canvas.getWidth() - patchX - 15; // ~567px
            int patchH = 326;

            BufferedImage patch = new BufferedImage(patchW, patchH, BufferedImage.TYPE_INT_ARGB);
            java.util.Random rand = new java.util.Random(42);
            int feather = 24;

            for (int y = 0; y < patchH; y++) {
                float vFactor = (float) y / patchH;
                for (int x = 0; x < patchW; x++) {
                    float hFactor = (float) x / patchW;

                    // Bilinear interpolation of the authentic sand wall tone
                    int rTop = (int) (192 + (232 - 192) * hFactor);
                    int gTop = (int) (140 + (224 - 140) * hFactor);
                    int bTop = (int) (92 + (206 - 92) * hFactor);

                    int rBot = (int) (195 + (233 - 195) * hFactor);
                    int gBot = (int) (145 + (198 - 145) * hFactor);
                    int bBot = (int) (90 + (148 - 90) * hFactor);

                    int r = (int) (rTop + (rBot - rTop) * vFactor);
                    int g = (int) (gTop + (gBot - gTop) * vFactor);
                    int b = (int) (bTop + (bBot - bTop) * vFactor);

                    // Organic plaster sand texture noise
                    int noise = rand.nextInt(7) - 3;
                    r = Math.min(255, Math.max(0, r + noise));
                    g = Math.min(255, Math.max(0, g + noise));
                    b = Math.min(255, Math.max(0, b + noise));

                    // Soft alpha edge feathering
                    int distLeft = x;
                    int distRight = patchW - 1 - x;
                    int distTop = y;
                    int distBot = patchH - 1 - y;

                    // Make top edge less feathered to cleanly wipe top headings
                    int effTopDist = distTop * 2;
                    int minDist = Math.min(Math.min(distLeft, distRight), Math.min(effTopDist, distBot));

                    int alpha = 255;
                    if (minDist < feather) {
                        alpha = (int) (255 * (minDist / (float) feather));
                    }

                    int argb = (alpha << 24) | (r << 16) | (g << 8) | b;
                    patch.setRGB(x, y, argb);
                }
            }

            g2d.drawImage(patch, patchX, patchY, null);

            // 2. Render real business categories & dishes from FinalMenuPlanDto
            List<CategoryPlanDto> categories = plan != null && plan.getCategories() != null
                    ? plan.getCategories()
                    : List.of();

            int col1X = 550;
            int col1W = 235;
            int col2X = 825;
            int col2W = 235;

            Font fontHeader = new Font("Serif", Font.BOLD, 13);
            Font fontItem = new Font("Serif", Font.BOLD, 11);
            Color colHeader = new Color(255, 255, 255); // Pure elegant white
            Color colItem = new Color(26, 20, 16); // Deep espresso

            if (categories.size() >= 4) {
                // Cat 1: Col 1 Top
                drawLuxuryInpaintCategory(g2d, categories.get(0), col1X, 56, col1W, 5, fontHeader, fontItem, colHeader, colItem);
                // Cat 2: Col 1 Bottom
                drawLuxuryInpaintCategory(g2d, categories.get(1), col1X, 226, col1W, 5, fontHeader, fontItem, colHeader, colItem);
                // Cat 3: Col 2 Top
                drawLuxuryInpaintCategory(g2d, categories.get(2), col2X, 56, col2W, 5, fontHeader, fontItem, colHeader, colItem);
                // Cat 4: Col 2 Bottom
                drawLuxuryInpaintCategory(g2d, categories.get(3), col2X, 226, col2W, 5, fontHeader, fontItem, colHeader, colItem);
            } else if (categories.size() == 3) {
                drawLuxuryInpaintCategory(g2d, categories.get(0), col1X, 56, col1W, 5, fontHeader, fontItem, colHeader, colItem);
                drawLuxuryInpaintCategory(g2d, categories.get(1), col1X, 226, col1W, 5, fontHeader, fontItem, colHeader, colItem);
                drawLuxuryInpaintCategory(g2d, categories.get(2), col2X, 56, col2W, 7, fontHeader, fontItem, colHeader, colItem);
            } else if (categories.size() == 2) {
                drawLuxuryInpaintCategory(g2d, categories.get(0), col1X, 56, col1W, 8, fontHeader, fontItem, colHeader, colItem);
                drawLuxuryInpaintCategory(g2d, categories.get(1), col2X, 56, col2W, 8, fontHeader, fontItem, colHeader, colItem);
            } else if (categories.size() == 1) {
                List<ProductPlanDto> items = getIncludedProducts(categories.get(0));
                int mid = (items.size() + 1) / 2;
                drawLuxuryInpaintList(g2d, categories.get(0).getCategoryName(), items.subList(0, mid), col1X, 56, col1W, 8, fontHeader, fontItem, colHeader, colItem);
                drawLuxuryInpaintList(g2d, "SPECIAL SELECTION", items.subList(mid, items.size()), col2X, 56, col2W, 8, fontHeader, fontItem, colHeader, colItem);
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("[COMPOSITE RENDERER] Error rendering luxury original style: {}", e.getMessage(), e);
            return null;
        }
    }

    private void drawLuxuryInpaintCategory(
            Graphics2D g2d,
            CategoryPlanDto category,
            int x, int startY, int width,
            int maxItems,
            Font fontHeader, Font fontItem,
            Color colHeader, Color colItem) {
        String catName = category != null && category.getCategoryName() != null
                ? category.getCategoryName().toUpperCase(Locale.ROOT)
                : "MENU";
        List<ProductPlanDto> prods = category != null ? getIncludedProducts(category) : List.of();
        drawLuxuryInpaintList(g2d, catName, prods, x, startY, width, maxItems, fontHeader, fontItem, colHeader, colItem);
    }

    private void drawLuxuryInpaintList(
            Graphics2D g2d,
            String categoryTitle,
            List<ProductPlanDto> products,
            int x, int startY, int width,
            int maxItems,
            Font fontHeader, Font fontItem,
            Color colHeader, Color colItem) {

        // Category Header
        g2d.setFont(fontHeader);
        g2d.setColor(colHeader);
        g2d.drawString(categoryTitle, x, startY);

        // Products
        g2d.setFont(fontItem);
        g2d.setColor(colItem);

        int currentY = startY + 24;
        int count = Math.min(products != null ? products.size() : 0, maxItems);

        for (int i = 0; i < count; i++) {
            ProductPlanDto prod = products.get(i);
            if (prod == null) continue;

            String name = formatProductNameWithGift(prod, 24);
            if (name.isBlank()) continue;

            // Hero badge hoặc Promo Gift badge
            if ("HERO".equalsIgnoreCase(prod.getDisplayGroup())) {
                g2d.setColor(new Color(220, 38, 38));
                g2d.fillOval(x - 9, currentY - 7, 5, 5);
            } else if (prod.getPromoGift() != null && !prod.getPromoGift().isBlank()) {
                g2d.setColor(new Color(217, 119, 6)); // Amber gift indicator
                g2d.fillOval(x - 9, currentY - 7, 5, 5);
            }

            Double price = prod.getFinalDisplayPrice();
            String formatted = formatPrice(price);
            FontMetrics fm = g2d.getFontMetrics(fontItem);
            FontMetrics fmPrice = g2d.getFontMetrics(fontItem);
            int priceW = fmPrice.stringWidth(formatted);
            int maxNameW = width - priceW - 10;

            Font currentFont = fontItem;
            if (fm.stringWidth(name) > maxNameW) {
                float targetSize = 11.0f;
                while (targetSize > 8.5f && fm.stringWidth(name) > maxNameW) {
                    targetSize -= 0.5f;
                    currentFont = fontItem.deriveFont(targetSize);
                    fm = g2d.getFontMetrics(currentFont);
                }
            }

            g2d.setFont(currentFont);
            g2d.setColor(colItem);
            g2d.drawString(name, x, currentY);

            // Right-aligned Price
            g2d.setFont(fontItem);
            int priceX = x + width - priceW;
            g2d.drawString(formatted, priceX, currentY);

            currentY += 20;
        }
    }

    // =========================================================================
    // 3. STYLE: TRUYEN_THONG (VINTAGE PARCHMENT THE BREW - 100% PRESERVED REFERENCE)
    // Preserves 100% of the authentic Vietnamese vintage parchment design (682x1024):
    // Header "TIỆM CÀ PHÊ THE BREW", rustic Indochine floral corners,
    // woodcut Hanoi Old Quarter house & Hoan Kiem lake, calligraphy subtitle.
    // Inpaints the 4 item zones and renders real business dishes & prices.
    // =========================================================================
    private byte[] renderVintageBrewOriginalStyle(
            byte[] referenceImageBytes,
            FinalMenuPlanDto plan,
            MenuStyleConfigDto styleConfig) {

        if (referenceImageBytes == null || referenceImageBytes.length == 0) {
            return null;
        }

        try {
            BufferedImage canvas = ImageIO.read(new ByteArrayInputStream(referenceImageBytes));
            if (canvas == null) return null;

            Graphics2D g2d = canvas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // 1. Inpaint the 4 old item list regions with authentic parchment texture
            // Cat 1 (CÀ PHÊ): X: 25..345, Y: 305..505
            inpaintParchment(g2d, 25, 305, 320, 200);
            // Cat 2 (TRÀ TRÁI CÂY): X: 350..665, Y: 305..505
            inpaintParchment(g2d, 350, 305, 315, 200);
            // Cat 3 (ĐÁ XAY & MATCHA): X: 25..345, Y: 570..695
            inpaintParchment(g2d, 25, 570, 320, 125);
            // Cat 4 (SIGNATURE): X: 350..665, Y: 570..695
            inpaintParchment(g2d, 350, 570, 315, 125);

            // 2. Render real business dishes & prices from FinalMenuPlanDto
            List<CategoryPlanDto> categories = plan != null && plan.getCategories() != null
                    ? plan.getCategories()
                    : List.of();

            Font fontItem = new Font("Serif", Font.BOLD, 12);
            Color colItem = new Color(35, 24, 18); // Deep sepia-black

            int col1X = 42;
            int col1Right = 325;
            int col2X = 368;
            int col2Right = 645;

            if (categories.size() >= 4) {
                drawVintageItemList(g2d, getIncludedProducts(categories.get(0)), col1X, 328, col1Right, 6, fontItem, colItem, false);
                drawVintageItemList(g2d, getIncludedProducts(categories.get(1)), col2X, 328, col2Right, 6, fontItem, colItem, false);
                drawVintageItemList(g2d, getIncludedProducts(categories.get(2)), col1X, 598, col1Right, 4, fontItem, colItem, false);
                drawVintageItemList(g2d, getIncludedProducts(categories.get(3)), col2X, 598, col2Right, 4, fontItem, colItem, true);
            } else if (categories.size() == 3) {
                drawVintageItemList(g2d, getIncludedProducts(categories.get(0)), col1X, 328, col1Right, 6, fontItem, colItem, false);
                drawVintageItemList(g2d, getIncludedProducts(categories.get(1)), col2X, 328, col2Right, 6, fontItem, colItem, false);
                drawVintageItemList(g2d, getIncludedProducts(categories.get(2)), col1X, 598, col1Right, 4, fontItem, colItem, false);
            } else if (categories.size() == 2) {
                drawVintageItemList(g2d, getIncludedProducts(categories.get(0)), col1X, 328, col1Right, 7, fontItem, colItem, false);
                drawVintageItemList(g2d, getIncludedProducts(categories.get(1)), col2X, 328, col2Right, 7, fontItem, colItem, false);
            } else if (categories.size() == 1) {
                List<ProductPlanDto> items = getIncludedProducts(categories.get(0));
                int mid = (items.size() + 1) / 2;
                drawVintageItemList(g2d, items.subList(0, mid), col1X, 328, col1Right, 7, fontItem, colItem, false);
                drawVintageItemList(g2d, items.subList(mid, items.size()), col2X, 328, col2Right, 7, fontItem, colItem, false);
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("[COMPOSITE RENDERER] Error rendering vintage brew original style: {}", e.getMessage(), e);
            return null;
        }
    }

    private void inpaintParchment(Graphics2D g2d, int x, int y, int w, int h) {
        BufferedImage patch = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.util.Random rand = new java.util.Random(42);
        int feather = 8;

        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int r = 233 + rand.nextInt(5) - 2;
                int g = 221 + rand.nextInt(5) - 2;
                int b = 204 + rand.nextInt(5) - 2;

                int distLeft = px;
                int distRight = w - 1 - px;
                int distTop = py;
                int distBot = h - 1 - py;
                int minDist = Math.min(Math.min(distLeft, distRight), Math.min(distTop, distBot));

                int alpha = 255;
                if (minDist < feather) {
                    alpha = (int) (255 * (minDist / (float) feather));
                }

                int argb = (alpha << 24) | (r << 16) | (g << 8) | b;
                patch.setRGB(px, py, argb);
            }
        }
        g2d.drawImage(patch, x, y, null);
    }

    private void drawVintageItemList(
            Graphics2D g2d,
            List<ProductPlanDto> products,
            int leftX, int startY, int rightX,
            int maxItems,
            Font fontItem, Color colItem,
            boolean isSignature) {

        g2d.setFont(fontItem);
        g2d.setColor(colItem);
        FontMetrics fm = g2d.getFontMetrics(fontItem);

        int currentY = startY;
        int count = Math.min(products != null ? products.size() : 0, maxItems);

        for (int i = 0; i < count; i++) {
            ProductPlanDto prod = products.get(i);
            if (prod == null) continue;

            String name = formatProductNameWithGift(prod, 24);
            if (name.isBlank()) continue;

            int itemX = leftX;
            if ("HERO".equalsIgnoreCase(prod.getDisplayGroup()) || isSignature) {
                g2d.setColor(new Color(180, 83, 9)); // Amber star
                g2d.drawString("★", leftX, currentY);
                itemX = leftX + 14;
            } else if (prod.getPromoGift() != null && !prod.getPromoGift().isBlank()) {
                g2d.setColor(new Color(180, 83, 9)); // Amber star
                g2d.drawString("♦", leftX, currentY);
                itemX = leftX + 14;
            }

            Double price = prod.getFinalDisplayPrice();
            String formatted = formatPriceWithoutCurrency(price);
            int priceW = fm.stringWidth(formatted);
            int maxNameW = (rightX - itemX) - priceW - 10;

            Font currentFont = fontItem;
            FontMetrics fmCurrent = fm;
            if (fmCurrent.stringWidth(name) > maxNameW) {
                float targetSize = 12.0f;
                while (targetSize > 9.0f && fmCurrent.stringWidth(name) > maxNameW) {
                    targetSize -= 0.5f;
                    currentFont = fontItem.deriveFont(targetSize);
                    fmCurrent = g2d.getFontMetrics(currentFont);
                }
            }

            g2d.setFont(currentFont);
            g2d.setColor(colItem);
            g2d.drawString(name, itemX, currentY);

            // Right-aligned Price formatted as e.g. 37.000
            g2d.setFont(fontItem);
            g2d.drawString(formatted, rightX - priceW, currentY);

            currentY += 26;
        }
    }

    private String formatPriceWithoutCurrency(Double price) {
        if (price == null || price == 0) return "0";
        if (price >= 1000) {
            DecimalFormat df = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.US));
            return df.format(price).replace(",", ".");
        } else {
            return String.format(Locale.US, "%.2f", price);
        }
    }

    // =========================================================================
    // 4. STYLE: TRE_TRUNG (YOUTHFUL TRENDY PASTEL MILK TEA & CAFE)
    // Matches reference: Bright warm ivory background, 3 REAL commercial Gen Z drinks
    // preserved from reference (Matcha, Brown Sugar Boba, Pink Berry Tea),
    // old text cleanly wiped, vibrant strawberry pill banner, perfectly positioned
    // rounded cards avoiding all 3 beverages.
    // =========================================================================
    // 4. STYLE: TRE_TRUNG (YOUTHFUL TRENDY PASTEL MILK TEA & CAFE - THE BREW TEMPLATE)
    // Matches reference: Warm beige/cream cafe canvas, giant aesthetic iced latte at top-right,
    // 4 real commercial beverages beside each category (Iced coffee, fruit tea, matcha, signature),
    // modern elegant brand box at top-left, and balanced pastel cards for 4 categories.
    // =========================================================================
    private void renderYouthfulStyle(
            Graphics2D g2d,
            byte[] referenceImageBytes,
            FinalMenuPlanDto plan,
            MenuStyleConfigDto styleConfig) {

        // 1. Draw the actual reference image containing the real commercial drinks
        if (referenceImageBytes != null && referenceImageBytes.length > 0) {
            try {
                BufferedImage refImg = ImageIO.read(new ByteArrayInputStream(referenceImageBytes));
                if (refImg != null) {
                    g2d.drawImage(refImg, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, null);
                }
            } catch (IOException e) {
                log.warn("[COMPOSITE RENDERER] Could not load youthful reference background: {}", e.getMessage());
            }
        } else {
            // Fallback soft ivory
            g2d.setColor(new Color(255, 253, 245));
            g2d.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        }

        // 2. Cleanly wipe the top-left area for Brand and Slogan (X: 25..375, Y: 25..385)
        g2d.setColor(new Color(253, 248, 242, 245));
        g2d.fillRoundRect(25, 25, 350, 360, 24, 24);
        g2d.setColor(new Color(251, 207, 232, 200));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(25, 25, 350, 360, 24, 24);

        // 3. Header: Vibrant Strawberry Pill Banner & Brand
        int bannerW = 300;
        int bannerH = 42;
        int bannerX = 50;
        int bannerY = 48;

        // Shadow behind pill
        g2d.setColor(new Color(225, 29, 72, 40));
        g2d.fillRoundRect(bannerX + 2, bannerY + 3, bannerW, bannerH, 20, 20);

        // Vibrant Rose/Pink pill
        GradientPaint pillGrad = new GradientPaint(
                bannerX, bannerY, new Color(244, 63, 94),
                bannerX + bannerW, bannerY, new Color(225, 29, 72)
        );
        g2d.setPaint(pillGrad);
        g2d.fillRoundRect(bannerX, bannerY, bannerW, bannerH, 20, 20);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2d.setColor(Color.WHITE);
        drawCenteredString(g2d, "✨ TRENDY DRINKS & CAFE ✨", bannerX + bannerW / 2, bannerY + 26);

        // Brand Title under banner
        String brandTitle = resolveBrandTitle(plan, "SWEET CORNER CAFE");
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2d.setColor(new Color(15, 23, 42)); // Modern navy
        g2d.drawString(brandTitle, bannerX, bannerY + 85);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(new Color(190, 24, 93)); // Deep pink
        g2d.drawString("♡ Tươi mát mỗi ngày • Ngọt ngào từng khoảnh khắc ♡", bannerX, bannerY + 115);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.setColor(new Color(100, 116, 139));
        g2d.drawString("Thưởng thức trọn vẹn hương vị tinh tế trong từng giọt cà phê", bannerX, bannerY + 140);

        // 4. Categories & Items in Modern Rounded Cards
        // Col 1 (Left): X=135, W=235, beside left drinks
        // Col 2 (Right): X=495, W=275, beside right drinks
        // Row 1: Y=475, Row 2: Y=695
        List<CategoryPlanDto> categories = plan != null && plan.getCategories() != null
                ? plan.getCategories()
                : List.of();

        if (categories.isEmpty()) return;

        int col1X = 135;
        int col1Width = 235;
        int col2X = 495;
        int col2Width = 275;

        Color headerColor = new Color(225, 29, 72); // Pink rose
        Color itemColor = new Color(30, 41, 59); // Modern navy black
        Color priceColor = new Color(219, 39, 119); // Bright magenta-pink

        renderYouthfulCategoryGrid(g2d, categories, col1X, col2X, col1Width, col2Width, 475, 695,
                headerColor, itemColor, priceColor);
    }

    // =========================================================================
    // HELPER: BALANCED CATEGORY GRID RENDERER (MODERN & LUXURY)
    // =========================================================================
    private void renderCategoryGrid(
            Graphics2D g2d,
            List<CategoryPlanDto> categories,
            int col1X, int col2X,
            int col1Width, int col2Width,
            int row1Y, int row2Y,
            Color headerColor,
            Color itemColor,
            Color priceColor,
            String fontName,
            boolean isSerif) {

        if (categories.size() <= 2) {
            int startY = row1Y + 20;
            if (categories.size() == 1) {
                CategoryPlanDto singleCat = categories.get(0);
                List<ProductPlanDto> items = getIncludedProducts(singleCat);
                int mid = (items.size() + 1) / 2;
                drawCategoryColumn(g2d, singleCat.getCategoryName(), items.subList(0, mid),
                        col1X, startY, col1Width, 8, 26, headerColor, itemColor, priceColor, fontName, isSerif);
                drawCategoryColumn(g2d, "MORE SPECIALTIES", items.subList(mid, items.size()),
                        col2X, startY, col2Width, 8, 26, headerColor, itemColor, priceColor, fontName, isSerif);
            } else {
                drawCategoryColumn(g2d, categories.get(0).getCategoryName(), getIncludedProducts(categories.get(0)),
                        col1X, startY, col1Width, 8, 26, headerColor, itemColor, priceColor, fontName, isSerif);
                drawCategoryColumn(g2d, categories.get(1).getCategoryName(), getIncludedProducts(categories.get(1)),
                        col2X, startY, col2Width, 8, 26, headerColor, itemColor, priceColor, fontName, isSerif);
            }
        } else {
            // 3 or 4 categories: 2x2 grid
            drawCategoryColumn(g2d, categories.get(0).getCategoryName(), getIncludedProducts(categories.get(0)),
                    col1X, row1Y, col1Width, 5, 24, headerColor, itemColor, priceColor, fontName, isSerif);
            drawCategoryColumn(g2d, categories.get(1).getCategoryName(), getIncludedProducts(categories.get(1)),
                    col2X, row1Y, col2Width, 5, 24, headerColor, itemColor, priceColor, fontName, isSerif);

            if (categories.size() >= 3) {
                drawCategoryColumn(g2d, categories.get(2).getCategoryName(), getIncludedProducts(categories.get(2)),
                        col1X, row2Y, col1Width, 5, 24, headerColor, itemColor, priceColor, fontName, isSerif);
            }
            if (categories.size() >= 4) {
                drawCategoryColumn(g2d, categories.get(3).getCategoryName(), getIncludedProducts(categories.get(3)),
                        col2X, row2Y, col2Width, 5, 24, headerColor, itemColor, priceColor, fontName, isSerif);
            }
        }
    }

    // =========================================================================
    // HELPER: YOUTHFUL ROUNDED CARD CATEGORY GRID
    // =========================================================================
    private void renderYouthfulCategoryGrid(
            Graphics2D g2d,
            List<CategoryPlanDto> categories,
            int col1X, int col2X,
            int col1Width, int col2Width,
            int row1Y, int row2Y,
            Color headerColor,
            Color itemColor,
            Color priceColor) {

        int cardHeight = 210;

        if (categories.size() <= 2) {
            int startY = row1Y + 10;
            int singleHeight = 420;
            if (categories.size() == 1) {
                CategoryPlanDto singleCat = categories.get(0);
                List<ProductPlanDto> items = getIncludedProducts(singleCat);
                int mid = (items.size() + 1) / 2;
                drawYouthfulCard(g2d, singleCat.getCategoryName(), items.subList(0, mid),
                        col1X, startY, col1Width, singleHeight, 10, headerColor, itemColor, priceColor);
                drawYouthfulCard(g2d, "HOT SPECIALS", items.subList(mid, items.size()),
                        col2X, startY, col2Width, singleHeight, 10, headerColor, itemColor, priceColor);
            } else {
                drawYouthfulCard(g2d, categories.get(0).getCategoryName(), getIncludedProducts(categories.get(0)),
                        col1X, startY, col1Width, singleHeight, 10, headerColor, itemColor, priceColor);
                drawYouthfulCard(g2d, categories.get(1).getCategoryName(), getIncludedProducts(categories.get(1)),
                        col2X, startY, col2Width, singleHeight, 10, headerColor, itemColor, priceColor);
            }
        } else {
            // 2x2 grid with white rounded cards
            drawYouthfulCard(g2d, categories.get(0).getCategoryName(), getIncludedProducts(categories.get(0)),
                    col1X, row1Y, col1Width, cardHeight, 6, headerColor, itemColor, priceColor);
            drawYouthfulCard(g2d, categories.get(1).getCategoryName(), getIncludedProducts(categories.get(1)),
                    col2X, row1Y, col2Width, cardHeight, 6, headerColor, itemColor, priceColor);

            if (categories.size() >= 3) {
                drawYouthfulCard(g2d, categories.get(2).getCategoryName(), getIncludedProducts(categories.get(2)),
                        col1X, row2Y, col1Width, cardHeight, 6, headerColor, itemColor, priceColor);
            }
            if (categories.size() >= 4) {
                drawYouthfulCard(g2d, categories.get(3).getCategoryName(), getIncludedProducts(categories.get(3)),
                        col2X, row2Y, col2Width, cardHeight, 6, headerColor, itemColor, priceColor);
            }
        }
    }

    private void drawYouthfulCard(
            Graphics2D g2d,
            String categoryName,
            List<ProductPlanDto> products,
            int x, int y, int width, int height,
            int maxItems,
            Color headerColor,
            Color itemColor,
            Color priceColor) {

        // Translucent warm ivory card with gentle shadow
        g2d.setColor(new Color(225, 29, 72, 15)); // Soft shadow
        g2d.fillRoundRect(x + 2, y + 3, width, height, 18, 18);

        g2d.setColor(new Color(254, 252, 248, 252)); // High-opacity warm ivory card
        g2d.fillRoundRect(x, y, width, height, 18, 18);

        g2d.setColor(new Color(254, 205, 211, 220)); // Light pink card border
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(x, y, width, height, 18, 18);

        // Header inside card
        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2d.setColor(headerColor);
        String title = "✿ " + (categoryName != null ? categoryName.toUpperCase(Locale.ROOT) : "MENU");
        g2d.drawString(title, x + 15, y + 26);

        // Divider
        g2d.setColor(new Color(254, 205, 211));
        g2d.fillRect(x + 15, y + 33, width - 30, 1);

        // Products
        int currentY = y + 55;
        int count = Math.min(products != null ? products.size() : 0, maxItems);
        Font fontProduct = new Font("SansSerif", Font.BOLD, 12);
        Font fontPrice = new Font("SansSerif", Font.BOLD, 12);
        for (int i = 0; i < count; i++) {
            ProductPlanDto prod = products.get(i);
            if (prod == null) continue;

            String name = formatProductNameWithGift(prod, 22);
            if (name.isBlank()) continue;

            String price = formatPrice(prod.getFinalDisplayPrice());
            FontMetrics fmPrice = g2d.getFontMetrics(fontPrice);
            int priceW = fmPrice.stringWidth(price);
            int maxNameW = width - 30 - priceW - 10;

            Font currentFont = fontProduct;
            FontMetrics fmItem = g2d.getFontMetrics(currentFont);
            if (fmItem.stringWidth(name) > maxNameW) {
                float targetSize = 12.0f;
                while (targetSize > 9.0f && fmItem.stringWidth(name) > maxNameW) {
                    targetSize -= 0.5f;
                    currentFont = fontProduct.deriveFont(targetSize);
                    fmItem = g2d.getFontMetrics(currentFont);
                }
            }

            g2d.setFont(currentFont);
            g2d.setColor(itemColor);
            g2d.drawString(name, x + 15, currentY);

            // Price
            g2d.setFont(fontPrice);
            g2d.setColor(priceColor);
            int priceX = x + width - 15 - priceW;
            g2d.drawString(price, priceX, currentY);

            // HERO Badge hoặc Promo Badge
            if ("HERO".equalsIgnoreCase(prod.getDisplayGroup())) {
                g2d.setColor(new Color(239, 68, 68));
                g2d.fillRoundRect(x + 15 - 10, currentY - 7, 5, 5, 2, 2);
            } else if (prod.getPromoGift() != null && !prod.getPromoGift().isBlank()) {
                g2d.setColor(new Color(234, 88, 12));
                g2d.fillRoundRect(x + 15 - 10, currentY - 7, 5, 5, 2, 2);
            }

            currentY += 24;
        }
    }

    // =========================================================================
    // HELPER: DRAW STANDARD CATEGORY COLUMN
    // =========================================================================
    private void drawCategoryColumn(
            Graphics2D g2d,
            String categoryName,
            List<ProductPlanDto> products,
            int x,
            int startY,
            int width,
            int maxItems,
            int rowHeight,
            Color headerColor,
            Color itemColor,
            Color priceColor,
            String fontName,
            boolean isSerif) {

        int currentY = startY;

        // Category Header
        g2d.setFont(new Font(fontName, Font.BOLD, isSerif ? 16 : 15));
        g2d.setColor(headerColor);
        String catTitle = categoryName != null ? categoryName.toUpperCase(Locale.ROOT) : "MENU";
        if (isSerif) {
            catTitle = "♦ " + catTitle + " ♦";
        }
        g2d.drawString(catTitle, x, currentY);

        // Thin underline
        g2d.fillRect(x, currentY + 5, width, 1);
        currentY += 25;

        // Product rows
        int count = Math.min(products != null ? products.size() : 0, maxItems);
        Font fontBase = new Font(fontName, Font.BOLD, 13);
        for (int i = 0; i < count; i++) {
            ProductPlanDto prod = products.get(i);
            if (prod == null) continue;

            String name = formatProductNameWithGift(prod, 24);
            if (name.isBlank()) continue;

            Double price = prod.getFinalDisplayPrice();
            String formattedPrice = formatPrice(price);
            FontMetrics fmPrice = g2d.getFontMetrics(fontBase);
            int priceWidth = fmPrice.stringWidth(formattedPrice);
            int maxNameW = width - priceWidth - 12;

            Font currentFont = fontBase;
            FontMetrics fmItem = g2d.getFontMetrics(currentFont);
            if (fmItem.stringWidth(name) > maxNameW) {
                float targetSize = 13.0f;
                while (targetSize > 9.5f && fmItem.stringWidth(name) > maxNameW) {
                    targetSize -= 0.5f;
                    currentFont = fontBase.deriveFont(targetSize);
                    fmItem = g2d.getFontMetrics(currentFont);
                }
            }

            // Draw Item Name
            g2d.setFont(currentFont);
            g2d.setColor(itemColor);
            g2d.drawString(name, x, currentY);

            // Draw Price right-aligned
            g2d.setFont(fontBase);
            g2d.setColor(priceColor);
            int priceX = x + width - priceWidth;
            g2d.drawString(formattedPrice, priceX, currentY);

            // Highlight HERO hoặc Promo items with a subtle accent icon
            if ("HERO".equalsIgnoreCase(prod.getDisplayGroup())) {
                g2d.setColor(isSerif ? new Color(180, 83, 9) : new Color(239, 68, 68));
                g2d.fillOval(x - 12, currentY - 8, 6, 6);
            } else if (prod.getPromoGift() != null && !prod.getPromoGift().isBlank()) {
                g2d.setColor(new Color(245, 158, 11)); // Amber icon
                g2d.fillOval(x - 12, currentY - 8, 6, 6);
            }

            currentY += rowHeight;
        }
    }

    // =========================================================================
    // HELPER: CROP & DRAW FOOD/BEVERAGE PHOTO FROM REFERENCE WITH SEAMLESS GRADIENT
    // =========================================================================
    private void drawReferenceVisual(
            Graphics2D g2d,
            byte[] referenceImageBytes,
            double cropRatio,
            int targetY,
            int targetHeight,
            Color blendBgColor) {

        if (referenceImageBytes == null || referenceImageBytes.length == 0) {
            return;
        }

        try {
            BufferedImage refImg = ImageIO.read(new ByteArrayInputStream(referenceImageBytes));
            if (refImg == null) return;

            int refW = refImg.getWidth();
            int refH = refImg.getHeight();

            // Crop strictly the bottom beverage photo to cleanly eliminate all old text
            int cropStartY = (int) (refH * cropRatio);
            int cropH = refH - cropStartY;

            BufferedImage bottomCrop = refImg.getSubimage(0, cropStartY, refW, cropH);

            // Draw cropped bottom beverage photography onto canvas
            g2d.drawImage(bottomCrop, 0, targetY, CANVAS_WIDTH, targetHeight, null);

            // Seamless soft gradient blend on the top edge of the photo matching the canvas background
            GradientPaint gradient = new GradientPaint(
                    0, targetY, new Color(blendBgColor.getRed(), blendBgColor.getGreen(), blendBgColor.getBlue(), 255),
                    0, targetY + 45, new Color(blendBgColor.getRed(), blendBgColor.getGreen(), blendBgColor.getBlue(), 0)
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, targetY, CANVAS_WIDTH, 45);

        } catch (IOException e) {
            log.warn("[COMPOSITE RENDERER] Could not blend reference beverage photo: {}", e.getMessage());
        }
    }

    // =========================================================================
    // HELPER: EMBLEMS, ORNAMENTS & FORMATTERS
    // =========================================================================
    private void drawGeometricEmblem(Graphics2D g2d, int cx, int cy, Color color) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(1.8f));

        int size = 18;
        g2d.drawPolygon(
                new int[]{cx, cx + size, cx, cx - size},
                new int[]{cy - size, cy, cy + size, cy},
                4
        );
        g2d.drawOval(cx - 7, cy - 7, 14, 14);
        g2d.fillOval(cx - 3, cy - 3, 6, 6);
    }

    private void drawRoyalCornerOrnaments(Graphics2D g2d, int x, int y, int w, int h) {
        g2d.setColor(new Color(180, 130, 72));
        g2d.setStroke(new BasicStroke(1.5f));
        int arm = 16;

        // Top-left
        g2d.drawLine(x + 4, y + 4, x + 4 + arm, y + 4);
        g2d.drawLine(x + 4, y + 4, x + 4, y + 4 + arm);

        // Top-right
        g2d.drawLine(x + w - 4, y + 4, x + w - 4 - arm, y + 4);
        g2d.drawLine(x + w - 4, y + 4, x + w - 4, y + 4 + arm);

        // Bottom-left
        g2d.drawLine(x + 4, y + h - 4, x + 4 + arm, y + h - 4);
        g2d.drawLine(x + 4, y + h - 4, x + 4, y + h - 4 - arm);

        // Bottom-right
        g2d.drawLine(x + w - 4, y + h - 4, x + w - 4 - arm, y + h - 4);
        g2d.drawLine(x + w - 4, y + h - 4, x + w - 4, y + h - 4 - arm);
    }

    private String resolveBrandTitle(FinalMenuPlanDto plan, String defaultTitle) {
        if (plan != null && plan.getSessionId() != null) {
            // Can be expanded to read business profile name if present
        }
        return defaultTitle;
    }

    private List<ProductPlanDto> getIncludedProducts(CategoryPlanDto category) {
        List<ProductPlanDto> list = new ArrayList<>();
        if (category != null && category.getProducts() != null) {
            for (ProductPlanDto p : category.getProducts()) {
                if (p != null && !Boolean.FALSE.equals(p.getIncludeInFinalMenu())) {
                    list.add(p);
                }
            }
        }
        return list;
    }

    private String formatPrice(Double price) {
        if (price == null || price == 0) return "0";
        if (price >= 1000) {
            DecimalFormat df = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.US));
            return df.format(price) + "đ";
        } else {
            return String.format(Locale.US, "%.2f", price);
        }
    }

    private void drawCenteredString(Graphics2D g2d, String text, int x, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, x - (textWidth / 2), y);
    }

    private String formatProductNameWithGift(ProductPlanDto prod, int maxLen) {
        if (prod == null || prod.getProductName() == null) return "";
        String name = prod.getProductName().trim();
        String gift = prod.getPromoGift();
        if (gift != null && !gift.isBlank()) {
            String cleanGift = gift.replace("Tặng ", "").replace("tặng ", "").trim();
            return name + " (Ưu đãi: " + cleanGift + ")";
        }
        return name;
    }
}
