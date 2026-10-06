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
public class RestructureItemDto {
    private String id;
    private String actionTitle;
    private String details;
    private List<String> basedOnStrategyIds;
}
