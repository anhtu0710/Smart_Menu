package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponseDto {

    /**
     *
     * ID phiên phân tích trong Database
     *
     * Map với:
     *
     * MenuAnalysisSession.Id
     *
     */
    private Long sessionId;

    /**
     *
     * Tên file gốc người dùng upload
     *
     */
    private String fileName;

    /**
     *
     * MIME type
     *
     * Ví dụ:
     *
     * application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     *
     */
    private String fileType;

    /**
     *
     * Dung lượng file
     *
     */
    private Long fileSize;

    /**
     *
     * Đường dẫn lưu file thật trên server
     *
     */
    private String filePath;

    /**
     *
     * Loại file
     *
     * BUSINESS_EXCEL
     *
     * OLD_MENU_IMAGE
     *
     */
    private String fileCategory;

}