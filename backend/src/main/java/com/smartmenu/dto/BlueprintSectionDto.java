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
public class BlueprintSectionDto {
    private String categoryName;
    private String priority; // 'HERO' | 'CORE' | 'NORMAL' | 'REDUCED'
    private List<ProductItemDto> dishes;
}
