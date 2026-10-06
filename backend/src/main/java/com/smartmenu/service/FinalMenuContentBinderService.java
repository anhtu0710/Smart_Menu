package com.smartmenu.service;

import com.smartmenu.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

@Service
@Slf4j
public class FinalMenuContentBinderService {

    /**
     * BIND NỘI DUNG BẰNG JAVA W3C DOM XML API:
     * Tìm element theo data-product-id="P001" và set textContent bằng DOM API.
     * TUYỆT ĐỐI KHÔNG DÙNG STRING REPLACE THÔ.
     */
    public String bindLockedContent(String sanitizedSvg, FinalMenuPlanDto plan) {
        if (sanitizedSvg == null || plan == null) {
            throw new IllegalArgumentException("Dữ liệu đầu vào Content Binding không hợp lệ");
        }

        try {
            Document doc = parseXmlSafely(sanitizedSvg);
            DecimalFormat priceFormatter = getPriceFormatter();

            // Build Product Map & Category Map từ FinalMenuPlanDto đã freeze
            Map<String, String> productNameMap = new HashMap<>();
            Map<String, Double> productPriceMap = new HashMap<>();
            Map<String, String> categoryNameMap = new HashMap<>();

            if (plan.getCategories() != null) {
                for (CategoryPlanDto cat : plan.getCategories()) {
                    categoryNameMap.put(cat.getCategoryId(), cat.getCategoryName());
                    if (cat.getProducts() != null) {
                        for (ProductPlanDto p : cat.getProducts()) {
                            productNameMap.put(p.getProductId(), p.getProductName());
                            productPriceMap.put(p.getProductId(), p.getFinalDisplayPrice());
                        }
                    }
                }
            }

            if (plan.getFinalMenuContent() != null && plan.getFinalMenuContent().getCategories() != null) {
                for (FinalMenuCategoryDto cat : plan.getFinalMenuContent().getCategories()) {
                    categoryNameMap.putIfAbsent(cat.getCategoryId(), cat.getCategoryName());
                    if (cat.getProducts() != null) {
                        for (FinalMenuProductDto p : cat.getProducts()) {
                            productNameMap.putIfAbsent(p.getProductId(), p.getProductName());
                            if (p.getFinalDisplayPrice() != null) {
                                productPriceMap.putIfAbsent(p.getProductId(), (double) p.getFinalDisplayPrice());
                            }
                        }
                    }
                }
            }

            // 1. Bind Category Names theo data-category-id
            NodeList allElements = doc.getElementsByTagName("*");
            for (int i = 0; i < allElements.getLength(); i++) {
                Node node = allElements.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;

                    String catId = el.getAttribute("data-category-id");
                    if (catId != null && categoryNameMap.containsKey(catId.trim())) {
                        String catName = categoryNameMap.get(catId.trim());
                        bindFieldInSubtree(el, "category-name", catName);
                    }

                    // 2. Bind Product Name & Price theo data-product-id
                    String prodId = el.getAttribute("data-product-id");
                    if (prodId != null && productNameMap.containsKey(prodId.trim())) {
                        String prodName = productNameMap.get(prodId.trim());
                        Double prodPrice = productPriceMap.get(prodId.trim());

                        bindFieldInSubtree(el, "name", prodName);

                        if (prodPrice != null) {
                            String formattedPrice = priceFormatter.format(prodPrice) + "đ";
                            bindFieldInSubtree(el, "price", formattedPrice);
                        }
                    }
                }
            }

            // Serialize DOM về lại String SVG XML
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            String boundSvg = writer.toString();
            log.info("[CONTENT BINDER SUCCESS] Đã tiêm chính xác Tên món & Giá tiền vào DOM SVG cho Session {}", plan.getSessionId());
            return boundSvg;

        } catch (Exception e) {
            log.error("Lỗi Content Binding DOM XML: {}", e.getMessage());
            throw new RuntimeException("Không thể tiêm dữ liệu vào SVG DOM", e);
        }
    }

    private void bindFieldInSubtree(Element parent, String fieldName, String value) {
        NodeList children = parent.getElementsByTagName("*");
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) child;
                String field = el.getAttribute("data-field");
                if (fieldName.equalsIgnoreCase(field)) {
                    el.setTextContent(value != null ? value : "");
                }
            }
        }
    }

    private Document parseXmlSafely(String xmlContent) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    private DecimalFormat getPriceFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,###", symbols);
    }
}
