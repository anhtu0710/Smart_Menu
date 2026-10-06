package com.smartmenu.controller;

import com.smartmenu.dto.ApiResponse;
import com.smartmenu.dto.UploadResponseDto;

import com.smartmenu.entity.MenuAnalysisSession;
import com.smartmenu.entity.Restaurant;
import com.smartmenu.entity.UploadedFile;

import com.smartmenu.repository.MenuAnalysisSessionRepository;
import com.smartmenu.repository.RestaurantRepository;
import com.smartmenu.repository.UploadedFileRepository;

import com.smartmenu.service.FileStorageService;
import com.smartmenu.service.SessionAccessService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final FileStorageService fileStorageService;

    private final MenuAnalysisSessionRepository sessionRepository;

    private final UploadedFileRepository uploadedFileRepository;

    private final RestaurantRepository restaurantRepository;

    private final SessionAccessService sessionAccessService;

    /**
     * Upload:
     * - Excel kinh doanh
     * - Ảnh menu cũ
     * - restaurantId (Tùy chọn)
     *
     * Sau upload tạo session thật được liên kết đúng với Restaurant
     */
    @PostMapping(value = "/menu-analysis", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UploadResponseDto>> uploadAnalysisFiles(
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestParam("menuImage") MultipartFile menuImage,
            @RequestParam(value = "restaurantId", required = false) Long restaurantId,
            HttpServletRequest request
    ) {

        if (excelFile == null || excelFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File Excel không được rỗng"));
        }

        if (menuImage == null || menuImage.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Ảnh menu không được rỗng"));
        }

        /*
         * 1. Xác định & Validate RestaurantId cho session này
         */
        Long finalRestaurantId = null;
        boolean restaurantExists = false;

        Long currentUserId = sessionAccessService.requireAuthenticatedUser(request);

        if (restaurantId != null) {
            sessionAccessService.requireRestaurantOwner(restaurantId, currentUserId);
            restaurantExists = true;
            log.info("Xác thực restaurantId request={}: exists={}", restaurantId, restaurantExists);

            if (!restaurantExists) {
                log.warn("Upload thất bại: Restaurant với ID={} không tồn tại trong hệ thống", restaurantId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Restaurant không tồn tại"));
            }
            finalRestaurantId = restaurantId;
        } else {
            // Tạo mới Restaurant riêng biệt cho session này
            Restaurant newRestaurant = Restaurant.builder()
                    .userId(currentUserId)
                    .restaurantName("Chưa xác định được thông tin thương hiệu")
                    .build();
            newRestaurant = restaurantRepository.save(newRestaurant);
            finalRestaurantId = newRestaurant.getId();
            restaurantExists = true;
            log.info("Đã khởi tạo Restaurant mới ID={} cho session upload (userId={})", finalRestaurantId, currentUserId);
        }

        // Logging đầy đủ trước khi lưu MenuAnalysisSession
        log.info("Lưu MenuAnalysisSession: restaurantId={}, restaurantExists={}, status={}",
                finalRestaurantId, restaurantExists, "UPLOADED");

        MenuAnalysisSession session = MenuAnalysisSession.builder()
                .status("UPLOADED")
                .restaurantId(finalRestaurantId)
                .build();

        session = sessionRepository.save(session);

        /*
         *
         * 2. Lưu Excel
         *
         */

        String excelPath =

                fileStorageService.storeFile(

                        excelFile,

                        "business_excel"

                );

        UploadedFile excel =

                UploadedFile.builder()

                        .sessionId(
                                session.getId())

                        .fileName(
                                excelFile.getOriginalFilename())

                        .filePath(
                                excelPath)

                        .fileType(
                                excelFile.getContentType())

                        .fileCategory(
                                "BUSINESS_EXCEL")

                        .fileSize(
                                excelFile.getSize())

                        .build();

        uploadedFileRepository.save(
                excel);

        /*
         *
         * 3. Lưu ảnh menu
         *
         */

        String imagePath =

                fileStorageService.storeFile(

                        menuImage,

                        "old_menu"

                );

        UploadedFile image =

                UploadedFile.builder()

                        .sessionId(
                                session.getId())

                        .fileName(
                                menuImage.getOriginalFilename())

                        .filePath(
                                imagePath)

                        .fileType(
                                menuImage.getContentType())

                        .fileCategory(
                                "OLD_MENU_IMAGE")

                        .fileSize(
                                menuImage.getSize())

                        .build();

        uploadedFileRepository.save(
                image);

        UploadResponseDto response =

                UploadResponseDto.builder()

                        .sessionId(
                                session.getId())

                        .fileName(
                                excelFile.getOriginalFilename())

                        .fileType(
                                excelFile.getContentType())

                        .fileSize(
                                excelFile.getSize())

                        .filePath(
                                excelPath)

                        .fileCategory(
                                "BUSINESS_EXCEL + OLD_MENU_IMAGE")

                        .build();

        return ResponseEntity.ok(

                ApiResponse.success(

                        response,

                        "Upload dữ liệu SmartMenu thành công"

                )

        );

    }

}
