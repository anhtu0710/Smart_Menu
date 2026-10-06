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
public class FinalMenuCategoryDto {
    private String categoryId;      // e.g. "CAT01"
    private String categoryName;    // e.g. "Cà phê"
    private Integer categoryOrder;  // 1, 2, 3...
    private List<FinalMenuProductDto> products;
}
