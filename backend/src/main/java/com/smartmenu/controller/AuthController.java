package com.smartmenu.controller;

import com.smartmenu.dto.ApiResponse;
import com.smartmenu.dto.LoginRequestDto;
import com.smartmenu.dto.RegisterRequestDto;
import com.smartmenu.dto.UserDto;
import com.smartmenu.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Yêu cầu đăng ký tài khoản cho email: {}", request != null ? request.getEmail() : null);
        ApiResponse<UserDto> response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto>> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest httpRequest) {
        log.info("Yêu cầu đăng nhập cho email: {}", request != null ? request.getEmail() : null);
        ApiResponse<UserDto> response = authService.login(request);

        if (response.isSuccess() && response.getData() != null && response.getData().getId() != null) {
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("AUTH_USER_ID", response.getData().getId());

            boolean remember = request.getRememberMe() != null && request.getRememberMe();
            session.setMaxInactiveInterval(remember ? 2592000 : 1800);
            log.info("Đã tạo HttpSession cho User ID: {}, timeout: {} giây", response.getData().getId(), session.getMaxInactiveInterval());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("AUTH_USER_ID");
            if (userId != null) {
                return ResponseEntity.ok(authService.getUserById(userId));
            }
        }
        return ResponseEntity.ok(ApiResponse.error("Chưa đăng nhập"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
            log.info("Đã hủy HttpSession thành công");
        }
        return ResponseEntity.ok(ApiResponse.success("Success", "Đăng xuất thành công"));
    }
}
