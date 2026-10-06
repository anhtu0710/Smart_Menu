package com.smartmenu.service;

import com.smartmenu.dto.ProductItemDto;
import com.smartmenu.entity.RawExcelData;
import com.smartmenu.repository.RawExcelDataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;

import java.nio.file.Path;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelParserService {

    private final RawExcelDataRepository rawExcelDataRepository;

    /**
     * Đọc file Excel thực tế
     */
    public List<ProductItemDto> parseExcel(

            Path filePath,

            Long sessionId,

            Long fileId

    ) {

        List<ProductItemDto> products = new ArrayList<>();

        try (
                InputStream inputStream = new FileInputStream(
                        filePath.toFile());

                Workbook workbook = WorkbookFactory.create(
                        inputStream)

        ) {

            for (Sheet sheet : workbook) {

                Row headerRow = null;
                Map<Integer, String> mapping = new HashMap<>();

                // Tìm dòng header chuẩn nhất (chứa PRODUCT_NAME và có nhiều cột khớp nhất)
                for (Row row : sheet) {
                    if (isEmpty(row)) continue;
                    Map<Integer, String> testMap = analyzeHeader(row);
                    if (testMap.containsValue("PRODUCT_NAME") && testMap.size() > mapping.size()) {
                        headerRow = row;
                        mapping = testMap;
                    }
                }

                if (headerRow == null) {
                    log.warn("Không tìm thấy dòng tiêu đề cột hợp lệ trong sheet: {}", sheet.getSheetName());
                    continue;
                }

                log.info("Đã tìm thấy dòng header tại dòng {} của sheet {}. Số cột khớp: {}", 
                        headerRow.getRowNum() + 1, sheet.getSheetName(), mapping.size());

                // Đọc các dòng dữ liệu nằm phía sau dòng header
                for (Row row : sheet) {
                    if (row.getRowNum() <= headerRow.getRowNum()) {
                        continue;
                    }

                    if (isEmpty(row)) {
                        continue;
                    }

                    Map<String, String> rowData = new HashMap<>();

                    for (Map.Entry<Integer, String> entry : mapping.entrySet()) {

                        String value = getValue(
                                row.getCell(
                                        entry.getKey()));

                        rowData.put(
                                entry.getValue(),
                                value);

                        saveRawData(

                                fileId,

                                sheet.getSheetName(),

                                row.getRowNum(),

                                entry.getValue(),

                                value

                        );

                    }

                    ProductItemDto product = convertProduct(
                            rowData);

                    /*
                     * Validation & Data Sanitization nghiêm ngặt:
                     * - Tên sản phẩm không null, không rỗng
                     * - Không phải placeholder / description / summary row
                     * - Loại bỏ trùng lặp (deduplicate)
                     */
                    if (isValidProductRow(product)) {
                        String normKey = normalize(product.getName());
                        boolean exists = products.stream().anyMatch(p -> normalize(p.getName()).equals(normKey));
                        if (!exists) {
                            products.add(product);
                        } else {
                            log.info("Bỏ qua sản phẩm trùng lặp: {}", product.getName());
                        }
                    }
                }
            }

            // Log debug chi tiết dữ liệu thực đọc từ Excel
            log.info("=== KẾT QUẢ PARSE EXCEL DỮ LIỆU KINH DOANH ===");
            for (ProductItemDto p : products) {
                double cost = p.getCostPrice();
                double profit = (p.getOriginalPrice() - cost) * p.getSalesQuantity();
                log.info("PRODUCT: {} | PRICE: {} | COST: {} | QUANTITY: {} | PROFIT: {}",
                        p.getName(), p.getOriginalPrice(), cost, p.getSalesQuantity(), profit);

                if (cost <= 0) {
                    log.warn("Không tìm thấy dữ liệu giá vốn từ Excel cho sản phẩm: {}", p.getName());
                }
            }

        } catch (Exception e) {

            log.error(
                    "Excel parsing error: ",
                    e);

            throw new RuntimeException(
                    "Không thể đọc dữ liệu Excel: " + e.getMessage(), e);

        }

        return products;

    }

    /**
     * Phân tích header Excel
     */
    private Map<Integer, String> analyzeHeader(
            Row headerRow) {

        Map<Integer, String> result = new HashMap<>();

        for (Cell cell : headerRow) {

            String header = normalize(
                    getValue(cell));

            if (match(
                    header,
                    "product name",
                    "product_name",
                    "productname",
                    "ten mon",
                    "mon an",
                    "san pham",
                    "product",
                    "dish",
                    "item",
                    "name",
                    "ten sp",
                    "ten hang",
                    "ten")) {

                result.put(
                        cell.getColumnIndex(),
                        "PRODUCT_NAME");

            } else if (match(
                    header,
                    "category",
                    "nhom mon",
                    "loai",
                    "group",
                    "type",
                    "danh muc",
                    "nhom",
                    "phan loai")) {

                result.put(
                        cell.getColumnIndex(),
                        "CATEGORY");

            } else if (match(
                    header,
                    "quantity sold",
                    "quantity_sold",
                    "quantitysold",
                    "so luong",
                    "sl ban",
                    "sl",
                    "quantity",
                    "qty",
                    "sold",
                    "sales",
                    "luot ban",
                    "da ban")) {

                result.put(
                        cell.getColumnIndex(),
                        "QUANTITY");

            } else if (match(
                    header,
                    "cost price",
                    "cost_price",
                    "costprice",
                    "gia von",
                    "cost",
                    "nguyen lieu",
                    "von",
                    "cogs",
                    "gia nhap",
                    "chi phi nguyen lieu",
                    "food cost",
                    "chi phi",
                    "gia thanh",
                    "chi phi nhap",
                    "nhap")) {

                result.put(
                        cell.getColumnIndex(),
                        "COST_PRICE");

            } else if (match(
                    header,
                    "selling price",
                    "selling_price",
                    "sellingprice",
                    "gia ban",
                    "don gia",
                    "price",
                    "selling",
                    "gia")) {

                result.put(
                        cell.getColumnIndex(),
                        "SELLING_PRICE");

            }

        }

        return result;

    }

    private ProductItemDto convertProduct(

            Map<String, String> data

    ) {

        return ProductItemDto.builder()

                .id(
                        UUID.randomUUID()
                                .toString())

                .name(
                        data.get(
                                "PRODUCT_NAME"))

                .category(
                        data.getOrDefault(
                                "CATEGORY",
                                "UNKNOWN"))

                .originalPrice(
                        parseNumber(
                                data.get(
                                        "SELLING_PRICE")))

                .costPrice(
                        parseNumber(
                                data.get(
                                        "COST_PRICE")))

                .salesQuantity(
                        (int) parseNumber(
                                data.get(
                                        "QUANTITY")))

                .build();

    }

    private void saveRawData(

            Long fileId,

            String sheetName,

            int row,

            String column,

            String value

    ) {

        RawExcelData raw = RawExcelData.builder()

                .fileId(fileId)

                .sheetName(sheetName)

                .rowNumber(row)

                .columnName(column)

                .columnValue(value)

                .build();

        rawExcelDataRepository.save(raw);

    }

    private String getValue(Cell cell) {

        if (cell == null) {

            return "";

        }

        DataFormatter formatter = new DataFormatter();

        return formatter.formatCellValue(
                cell);

    }

    private boolean isEmpty(Row row) {

        for (Cell cell : row) {

            if (!getValue(cell)
                    .isBlank()) {

                return false;

            }

        }

        return true;

    }

    private boolean match(

            String text,

            String... keywords

    ) {

        for (String keyword : keywords) {

            if (text.contains(keyword)) {

                return true;

            }

        }

        return false;

    }

    private String normalize(
            String value) {

        if (value == null) {

            return "";

        }

        String temp = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String decompressed = pattern.matcher(temp).replaceAll("");

        return decompressed

                .toLowerCase()

                .replace(
                        "đ",
                        "d")

                .replace(
                        "Đ",
                        "d")

                .replace(
                        "_",
                        " ")

                .replaceAll(
                        "[^a-z0-9 ]",
                        "")

                .trim();

    }

    private double parseNumber(
            String value) {

        if (value == null
                ||
                value.isBlank()) {

            return 0;

        }

        try {

            String clean = value.replaceAll("[^0-9.,]", "").trim();
            if (clean.isEmpty()) return 0;

            if (clean.contains(".") && clean.contains(",")) {
                if (clean.lastIndexOf(".") < clean.lastIndexOf(",")) {
                    clean = clean.replace(".", "").replace(",", ".");
                } else {
                    clean = clean.replace(",", "");
                }
            } else if (clean.contains(".")) {
                int lastDot = clean.lastIndexOf(".");
                if (clean.length() - lastDot - 1 == 3 && clean.indexOf(".") == lastDot) {
                    clean = clean.replace(".", "");
                }
            } else if (clean.contains(",")) {
                int lastComma = clean.lastIndexOf(",");
                if (clean.length() - lastComma - 1 == 3 && clean.indexOf(",") == lastComma) {
                    clean = clean.replace(",", "");
                } else {
                    clean = clean.replace(",", ".");
                }
            }

            return Double.parseDouble(clean);

        } catch (Exception e) {

            return 0;

        }

    }

    /**
     * Data Sanitization & Placeholder Detection:
     * Kiểm tra xem 1 row có thực sự là món ăn / sản phẩm hợp lệ hay không.
     * Bỏ qua các dòng tiêu đề rác, ghi chú, mô tả menu, placeholder hoặc tổng cộng.
     */
    private boolean isValidProductRow(ProductItemDto p) {
        if (p == null || p.getName() == null) return false;

        String name = p.getName().trim();
        if (name.isEmpty() || name.length() < 2) return false;

        String lower = name.toLowerCase();

        // Danh sách từ khóa rác / heading / note / placeholder
        List<String> garbageKeywords = List.of(
                "các món tương ứng", "danh sách món", "món tương ứng", "menu bếp việt",
                "bếp việt", "các món trong menu", "menu ", "tổng cộng", "tổng ", "tong cong",
                "ghi chú", "bảng kê", "stt", "tên món", "tên sản phẩm", "đơn vị tính",
                "thành tiền", "nguyên liệu", "mô tả", "hướng dẫn", "stt ", "danh mục"
        );

        for (String kw : garbageKeywords) {
            if (lower.contains(kw)) {
                log.warn("Đã bỏ qua dòng rác / placeholder không phải sản phẩm: '{}'", name);
                return false;
            }
        }

        // Bắt buộc sản phẩm phải có dữ liệu kinh doanh hợp lệ (giá bán > 0 HOẶC sản lượng > 0 HOẶC giá vốn > 0)
        if (p.getOriginalPrice() <= 0 && p.getCostPrice() <= 0 && p.getSalesQuantity() <= 0) {
            log.warn("Đã bỏ qua dòng không có giá bán, giá vốn và sản lượng bán: '{}'", name);
            return false;
        }

        return true;
    }

}