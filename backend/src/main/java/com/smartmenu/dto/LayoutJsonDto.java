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
public class LayoutJsonDto {
    private String templateId;
    private List<ContentBlockDto> contentBlocks;
    private List<ExclusionZoneDto> exclusionZones;
    private int fontSize;
    private double lineHeight;
    private int columns;
    private String textColor;
    private String accentColor;
    private String fontFamily;
}
