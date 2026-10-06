package com.smartmenu.service;

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
import java.util.Set;

@Service
@Slf4j
public class FinalMenuSvgSanitizerService {

    private static final Set<String> ALLOWED_SVG_TAGS = Set.of(
            "svg", "g", "text", "tspan", "path", "rect", "circle", "line",
            "ellipse", "polyline", "polygon", "defs", "lineargradient", "radialgradient", "stop", "style"
    );

    private static final Set<String> ALLOWED_FONTS = Set.of(
            "dejavu sans", "liberation sans", "noto sans", "arial", "sansserif", "serif", "sans-serif"
    );

    /**
     * BẢO MẬT CHẶN XXE NGAY TỪ LÚC KHI TẠO PARSER XML:
     * Disable DOCTYPE, external entities, external DTD, XInclude trước khi parse.
     */
    public String sanitizeSvg(String rawSvgContent) {
        if (rawSvgContent == null || rawSvgContent.isBlank()) {
            throw new IllegalArgumentException("SVG content không được để trống");
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            
            // XXE Security Features
            try {
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (Exception e) {
                log.warn("Một số XXE features không được hỗ trợ bởi XML Parser hiện tại: {}", e.getMessage());
            }
            
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(rawSvgContent.getBytes(StandardCharsets.UTF_8)));

            // Sanitize DOM Tree
            Element root = doc.getDocumentElement();
            sanitizeElementRecursive(root);

            // Serialize DOM về lại String SVG XML
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            String sanitized = writer.toString();
            log.info("[SVG SANITIZER SUCCESS] Đã sanitize SVG XML an toàn (XXE Disabled, Fonts Whitelisted)");
            return sanitized;

        } catch (Exception e) {
            log.error("Lỗi parse/sanitize SVG XML: {}", e.getMessage());
            throw new IllegalArgumentException("SVG XML không hợp lệ hoặc chứa mã không an toàn", e);
        }
    }

    private void sanitizeElementRecursive(Node node) {
        if (node == null) return;

        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) child;
                String tagName = el.getTagName().toLowerCase();

                // 1. Kiểm tra allowlist SVG tags
                if (!ALLOWED_SVG_TAGS.contains(tagName) || "script".equals(tagName) || "foreignobject".equals(tagName)) {
                    log.warn("[SVG SANITIZER] Removing unsafe tag: <{}>", tagName);
                    node.removeChild(child);
                    continue;
                }

                // 2. Chặn event handlers & javascript: attributes
                NamedNodeMap attributes = el.getAttributes();
                for (int j = attributes.getLength() - 1; j >= 0; j--) {
                    Node attr = attributes.item(j);
                    String attrName = attr.getNodeName().toLowerCase();
                    String attrVal = attr.getNodeValue() != null ? attr.getNodeValue().toLowerCase() : "";

                    if (attrName.startsWith("on") || attrVal.contains("javascript:")) {
                        log.warn("[SVG SANITIZER] Removing unsafe attribute: {}='{}'", attrName, attrVal);
                        el.removeAttribute(attr.getNodeName());
                    }

                    // Enforce font whitelist trên font-family attribute
                    if ("font-family".equals(attrName)) {
                        if (!ALLOWED_FONTS.contains(attrVal.trim())) {
                            el.setAttribute("font-family", "DejaVu Sans");
                        }
                    }
                }

                // 3. Xử lý thẻ <style>: Chặn @import, url(), external fonts
                if ("style".equals(tagName)) {
                    String styleText = el.getTextContent();
                    if (styleText != null) {
                        String cleanedStyle = styleText
                                .replaceAll("(?i)@import[^;]+;", "")
                                .replaceAll("(?i)url\\([^)]+\\)", "none")
                                .replaceAll("(?i)javascript:", "");
                        el.setTextContent(cleanedStyle);
                    }
                }

                // Đệ quy cho element con
                sanitizeElementRecursive(child);
            }
        }
    }
}
