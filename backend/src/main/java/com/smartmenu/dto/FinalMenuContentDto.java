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
public class FinalMenuContentDto {
    private String sessionId;
    private List<FinalMenuCategoryDto> categories;
    private List<FinalMenuComboDto> combos;
}
