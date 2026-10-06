package com.smartmenu.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResponseDto {
    private Long paymentId;
    private String transactionCode;
    private BigDecimal amount;
    private String qrImageUrl; // Đường dẫn ảnh QR cố định của bạn
    private String status;
    private String bankInfo;
}
