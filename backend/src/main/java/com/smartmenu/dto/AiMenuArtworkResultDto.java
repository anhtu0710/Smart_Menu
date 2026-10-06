package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMenuArtworkResultDto {
    private boolean success;
    private String imageUrl; // URL đến ảnh PNG đã tạo: /generated/menu_999.png hoặc data URI
    private ArtworkValidationResultDto validation;
    private AiArtworkManifestDto renderManifest;
    private String errorMessage;
}
