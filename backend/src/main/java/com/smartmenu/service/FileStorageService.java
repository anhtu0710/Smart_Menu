package com.smartmenu.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path storageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public FileStorageService() {
        try {
            Files.createDirectories(this.storageLocation);
        } catch (Exception e) {
            log.error("Không thể tạo thư mục lưu trữ uploads", e);
        }
    }

    public String storeFile(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file_" + System.currentTimeMillis();
        String fileExtension = "";
        int dotIdx = originalFileName.lastIndexOf('.');
        if (dotIdx > 0) {
            fileExtension = originalFileName.substring(dotIdx);
        }

        String uniqueFileName = category.toLowerCase() + "_" + UUID.randomUUID().toString() + fileExtension;
        Path targetPath = this.storageLocation.resolve(uniqueFileName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Lỗi khi lưu file {}: {}", originalFileName, e.getMessage());
            return targetPath.toAbsolutePath().toString();
        }
    }
}
