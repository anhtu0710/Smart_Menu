package com.smartmenu.service;

import com.smartmenu.entity.DataMapping;
import com.smartmenu.repository.DataMappingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataMappingService {

    private final DataMappingRepository dataMappingRepository;

    /**
     *
     * Mapping header Excel
     *
     */
    public Map<String, String> mappingColumns(

            List<String> headers,

            Long sessionId

    ) {

        Map<String, String> result = new HashMap<>();

        for (String header : headers) {

            String normalized = normalize(header);

            String mappedField = detectField(
                    normalized);

            if (mappedField != null) {

                result.put(

                        header,

                        mappedField

                );

                saveMapping(

                        sessionId,

                        header,

                        mappedField

                );

            }

        }

        return result;

    }

    /**
     *
     * Xác định loại dữ liệu
     *
     */
    private String detectField(

            String text

    ) {

        /*
         * Tên món
         */

        if (match(

                text,

                "ten mon",

                "mon an",

                "san pham",

                "product",

                "item",

                "dish",

                "name"

        )) {

            return "PRODUCT_NAME";

        }

        /*
         * Nhóm món
         */

        if (match(

                text,

                "nhom",

                "loai",

                "category",

                "group",

                "type"

        )) {

            return "CATEGORY";

        }

        /*
         * Số lượng bán
         */

        if (match(

                text,

                "so luong",

                "sl",

                "quantity",

                "qty",

                "sold",

                "sales"

        )) {

            return "QUANTITY";

        }

        /*
         * Giá bán
         */

        if (match(

                text,

                "gia ban",

                "don gia",

                "price",

                "selling",

                "amount"

        )) {

            return "SELLING_PRICE";

        }

        /*
         * Giá vốn
         */

        if (match(

                text,

                "gia von",

                "cost",

                "cost price",

                "ingredient"

        )) {

            return "COST_PRICE";

        }

        return null;

    }

    /**
     *
     * Lưu lịch sử mapping
     *
     */
    private void saveMapping(

            Long sessionId,

            String originalColumn,

            String mappedField

    ) {

        DataMapping mapping =

                DataMapping.builder()

                        .sessionId(

                                sessionId

                        )

                        .originalColumn(

                                originalColumn

                        )

                        .mappedField(

                                mappedField

                        )

                        .confidence(

                                calculateConfidence(
                                        originalColumn,
                                        mappedField)

                        )

                        .build();

        dataMappingRepository.save(
                mapping);

    }

    /**
     *
     * Độ tin cậy mapping
     *
     */
    private Double calculateConfidence(

            String original,

            String mapped

    ) {

        String text = normalize(
                original);

        if (mapped.equals("PRODUCT_NAME")
                &&
                (text.contains("name")
                        ||
                        text.contains("mon")
                        ||
                        text.contains("sanpham"))) {

            return 0.95;

        }

        if (mapped.equals("QUANTITY")
                &&
                (text.contains("quantity")
                        ||
                        text.contains("luong")
                        ||
                        text.contains("qty"))) {

            return 0.95;

        }

        if (mapped.equals("SELLING_PRICE")
                &&
                text.contains("price")) {

            return 0.95;

        }

        return 0.80;

    }

    private boolean match(

            String source,

            String... keywords

    ) {

        for (String keyword : keywords) {

            if (source.contains(keyword)) {

                return true;

            }

        }

        return false;

    }

    private String normalize(

            String value

    ) {

        if (value == null) {

            return "";

        }

        return value

                .toLowerCase()

                .replace(
                        "đ",
                        "d")

                .replaceAll(
                        "[^a-z0-9 ]",
                        "")

                .trim();

    }

}