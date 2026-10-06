package com.smartmenu.controller;

import com.smartmenu.dto.ApiResponse;
import com.smartmenu.dto.PaymentCreateResponseDto;
import com.smartmenu.exception.ForbiddenException;
import com.smartmenu.service.PaymentService;
import com.smartmenu.service.SessionAccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final SessionAccessService sessionAccessService;

    @Value("${app.payment.mock-enabled:false}")
    private boolean mockPaymentEnabled;

    @Value("${app.payment.webhook-token:}")
    private String webhookToken;

    /**
     * API tạo thanh toán
     */
    @PostMapping({"/api/payment/create", "/api/v1/payment/create"})
    public ResponseEntity<ApiResponse<PaymentCreateResponseDto>> createPayment(
            HttpServletRequest request
    ) {
        Long userId = sessionAccessService.requireAuthenticatedUser(request);
        PaymentCreateResponseDto response = paymentService.createPayment(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Khởi tạo giao dịch thành công"));
    }

    /**
     * Webhook Callback thanh toán từ Ngân hàng / Cổng thanh toán (SePay / Casso / Ngân hàng thật)
     */
    @PostMapping({"/api/payment/callback", "/api/v1/payment/callback"})
    public ResponseEntity<ApiResponse<String>> paymentCallback(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "X-SmartMenu-Webhook-Token", required = false) String token
    ) {
        if (webhookToken == null || webhookToken.isBlank() || !webhookToken.equals(token)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Webhook không được xác thực"));
        }
        log.info("[PAYMENT-WEBHOOK-RECEIVED] Webhook nhận được với các trường: {}",
                payload == null ? "[]" : payload.keySet());
        boolean ok = paymentService.handleWebhookCallback(payload);
        if (ok) {
            return ResponseEntity.ok(ApiResponse.success("SUCCESS", "Xác nhận thanh toán tiền thật thành công"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy giao dịch hoặc nội dung không hợp lệ"));
        }
    }

    /**
     * API Polling kiểm tra trạng thái thanh toán theo paymentId
     */
    @GetMapping({"/api/payment/check-status/{paymentId}", "/api/v1/payment/check-status/{paymentId}"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStatus(
            @PathVariable Long paymentId,
            HttpServletRequest request
    ) {
        Long userId = sessionAccessService.requireAuthenticatedUser(request);
        if (!paymentService.belongsToUser(paymentId, userId)) {
            throw new ForbiddenException("Bạn không có quyền xem giao dịch này");
        }
        Map<String, Object> status = paymentService.checkStatus(paymentId);
        return ResponseEntity.ok(ApiResponse.success(status, "Lấy trạng thái thành công"));
    }

    /**
     * API xác nhận thanh toán demo/test nhanh trực tiếp trên giao diện
     */
    @PostMapping({"/api/payment/mock-confirm/{paymentId}", "/api/v1/payment/mock-confirm/{paymentId}"})
    public ResponseEntity<ApiResponse<String>> mockConfirm(
            @PathVariable Long paymentId,
            HttpServletRequest request
    ) {
        if (!mockPaymentEnabled) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy API"));
        }
        Long userId = sessionAccessService.requireAuthenticatedUser(request);
        if (!paymentService.belongsToUser(paymentId, userId)) {
            throw new ForbiddenException("Bạn không có quyền cập nhật giao dịch này");
        }
        boolean ok = paymentService.mockConfirmPayment(paymentId);
        if (ok) {
            return ResponseEntity.ok(ApiResponse.success("SUCCESS", "Xác nhận thanh toán thành công"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy giao dịch"));
        }
    }
}
