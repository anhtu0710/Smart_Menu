package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalMenuResponseDto {
    private String finalImageUrl;
    private String templateId;
    private int totalDishesCount;
    private int canvasWidth;
    private int canvasHeight;
    private double aspectRatio;
    private List<RenderManifestItemDto> manifest;
    private boolean validated;
}
