package com.smartmenu.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Base64;

@Service
@Slf4j
public class MenuImageService {

        /**
         *
         * Convert ảnh menu sang Base64
         *
         * Dùng để gửi Gemini Vision
         *
         */
        public String encodeFileToBase64(

                        Path imagePath

        ) {

                if (imagePath == null) {

                        throw new IllegalArgumentException(
                                        "Đường dẫn ảnh menu không tồn tại");

                }

                if (!Files.exists(imagePath)) {

                        throw new IllegalArgumentException(
                                        "Không tìm thấy file menu");

                }

                try {

                        byte[] imageBytes =

                                        Files.readAllBytes(
                                                        imagePath);

                        return Base64

                                        .getEncoder()

                                        .encodeToString(
                                                        imageBytes);

                } catch (IOException e) {

                        log.error(
                                        "Không thể đọc ảnh menu: {}",
                                        e.getMessage());

                        throw new RuntimeException(
                                        "Lỗi xử lý ảnh menu");

                }

        }

        /**
         *
         * Kiểm tra ảnh menu hợp lệ
         *
         */
        public void validateMenuImage(

                        Path imagePath

        ) {

                if (imagePath == null
                                ||
                                !Files.exists(imagePath)) {

                        throw new IllegalArgumentException(
                                        "Ảnh menu không tồn tại");

                }

                try {

                        long size =

                                        Files.size(
                                                        imagePath);

                        /*
                         * Ảnh quá nhỏ
                         * không đủ cho Vision
                         */

                        if (size < 5 * 1024) {

                                throw new IllegalArgumentException(

                                                "Ảnh menu quá nhỏ, vui lòng upload ảnh rõ hơn"

                                );

                        }

                        String fileName =

                                        imagePath

                                                        .getFileName()

                                                        .toString()

                                                        .toLowerCase();

                        boolean validFormat =

                                        fileName.endsWith(".jpg")

                                                        ||

                                                        fileName.endsWith(".jpeg")

                                                        ||

                                                        fileName.endsWith(".png")

                                                        ||

                                                        fileName.endsWith(".webp")

                                                        ||

                                                        fileName.endsWith(".pdf");

                        if (!validFormat) {

                                throw new IllegalArgumentException(

                                                "Chỉ hỗ trợ JPG, JPEG, PNG, WEBP, PDF"

                                );

                        }

                } catch (IOException e) {

                        throw new RuntimeException(

                                        "Không thể kiểm tra ảnh menu"

                        );

                }

        }

        /**
         *
         * Lấy MIME type của ảnh
         *
         * Phục vụ Gemini Vision
         *
         */
        public String getMimeType(

                        Path imagePath

        ) {

                try {

                        String mime =

                                        Files.probeContentType(
                                                        imagePath);

                        if (mime != null) {

                                return mime;

                        }

                } catch (IOException ignored) {
                }

                return "image/jpeg";

        }

}