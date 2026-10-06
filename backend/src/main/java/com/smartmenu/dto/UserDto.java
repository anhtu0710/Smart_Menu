package com.smartmenu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private String avatarUrl;
    private String createdAt;
    private Boolean freeUsed;
}
