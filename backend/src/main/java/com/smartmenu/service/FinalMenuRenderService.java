package com.smartmenu.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class FinalMenuRenderService {

    /**
     * RENDER RASTER IMAGE (PNG) TỪ BOUND SVG STRUCTURED DESIGN
     * Sử dụng Apache Batik High DPI Transcoder
     */
    public String renderSvgToPngBase64(String boundSvg, int width, int height) {
        if (boundSvg == null || boundSvg.isBlank()) {
            throw new IllegalArgumentException("Nội dung Bound SVG không được để trống");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PNGTranscoder transcoder = new PNGTranscoder();

            // High DPI Scaling
            if (width > 0) transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) (width * 2));
            if (height > 0) transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) (height * 2));

            TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(boundSvg.getBytes(StandardCharsets.UTF_8)));
            TranscoderOutput output = new TranscoderOutput(baos);

            transcoder.transcode(input, output);

            byte[] pngBytes = baos.toByteArray();
            boolean validPngSignature = pngBytes.length >= 8 &&
                    (pngBytes[0] & 0xFF) == 0x89 &&
                    (pngBytes[1] & 0xFF) == 0x50 &&
                    (pngBytes[2] & 0xFF) == 0x4E &&
                    (pngBytes[3] & 0xFF) == 0x47 &&
                    (pngBytes[4] & 0xFF) == 0x0D &&
                    (pngBytes[5] & 0xFF) == 0x0A &&
                    (pngBytes[6] & 0xFF) == 0x1A &&
                    (pngBytes[7] & 0xFF) == 0x0A;

            String rawBase64 = Base64.getEncoder().encodeToString(pngBytes).replaceAll("[\\r\\n\\s]", "");
            String base64DataUri = "data:image/png;base64," + rawBase64;

            log.info("[FINAL-IMAGE] pngBytes={} validPngSignature={} base64Length={}",
                    pngBytes.length, validPngSignature, base64DataUri.length());

            if (!validPngSignature) {
                throw new IllegalStateException("PNG Signature không hợp lệ sau khi rasterize!");
            }

            return base64DataUri;

        } catch (Exception e) {
            log.error("Lỗi rasterize SVG sang PNG Base64: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi render hình ảnh Final Menu từ SVG", e);
        }
    }
}
