package com.smartmenu.service;

import com.smartmenu.dto.ApiResponse;
import com.smartmenu.dto.LoginRequestDto;
import com.smartmenu.dto.RegisterRequestDto;
import com.smartmenu.dto.UserDto;
import com.smartmenu.entity.User;
import com.smartmenu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApiResponse<UserDto> register(RegisterRequestDto request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return ApiResponse.error("Email không được để trống");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ApiResponse.error("Mật khẩu không được để trống");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            return ApiResponse.error("Họ tên không được để trống");
        }

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            return ApiResponse.error("Email đã tồn tại");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(email)
                .passwordHash(hashedPassword)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            User savedUser = userRepository.save(user);
            log.info("Đã đăng ký tài khoản thành công cho email: {}", email);
            return ApiResponse.success(mapToUserDto(savedUser), "Đăng ký tài khoản thành công");
        } catch (DataIntegrityViolationException e) {
            log.warn("Lỗi DataIntegrityViolationException khi tạo user email {}: {}", email, e.getMessage());
            return ApiResponse.error("Email đã tồn tại");
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi đăng ký tài khoản: {}", e.getMessage(), e);
            return ApiResponse.error("Không thể đăng ký tài khoản. Vui lòng thử lại sau.");
        }
    }

    public ApiResponse<UserDto> login(LoginRequestDto request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return ApiResponse.error("Email không được để trống");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ApiResponse.error("Mật khẩu không được để trống");
        }

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Đăng nhập thất bại cho email: {}", email);
            return ApiResponse.error("Email hoặc mật khẩu không chính xác");
        }

        log.info("Đăng nhập thành công cho email: {}", email);
        return ApiResponse.success(mapToUserDto(user), "Đăng nhập thành công");
    }

    public ApiResponse<UserDto> getUserById(Long userId) {
        if (userId == null) {
            return ApiResponse.error("Chưa đăng nhập");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.error("Không tìm thấy thông tin người dùng");
        }
        return ApiResponse.success(mapToUserDto(user), "Lấy thông tin người dùng thành công");
    }

    public UserDto mapToUserDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatarUrl())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .freeUsed(Boolean.TRUE.equals(user.getFreeUsed()))
                .build();
    }
}
