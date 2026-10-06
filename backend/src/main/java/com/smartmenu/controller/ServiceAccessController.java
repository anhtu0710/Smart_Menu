package com.smartmenu.controller;

import com.smartmenu.dto.AccessCheckResponseDto;
import com.smartmenu.dto.ApiResponse;
import com.smartmenu.service.PaymentService;
import com.smartmenu.service.SessionAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessController {

    private final PaymentService paymentService;
    private final SessionAccessService sessionAccessService;

    /**
     * API kiểm tra quyền sử dụng dịch vụ thiết kế menu
     * Hỗ trợ cả 2 đường dẫn:
     * - GET /api/service/check-access
     * - GET /api/v1/service/check-access
     */
    @GetMapping({"/api/service/check-access", "/api/v1/service/check-access"})
    public ResponseEntity<ApiResponse<AccessCheckResponseDto>> checkAccess(
            HttpServletRequest request
    ) {
        Long userId = sessionAccessService.requireAuthenticatedUser(request);
        AccessCheckResponseDto result = paymentService.checkAccess(userId);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }
}
