package com.smartmenu.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessCheckResponseDto {
    private boolean allowed;
    private String type; // "FREE", "PAID", "PAYMENT_REQUIRED"
    private String message;
    private Long activePaymentId;
}
