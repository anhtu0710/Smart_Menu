package com.smartmenu.service;

import com.smartmenu.entity.MenuAnalysisSession;
import com.smartmenu.entity.Restaurant;
import com.smartmenu.exception.ForbiddenException;
import com.smartmenu.exception.UnauthorizedException;
import com.smartmenu.repository.MenuAnalysisSessionRepository;
import com.smartmenu.repository.RestaurantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionAccessService {

    private final MenuAnalysisSessionRepository sessionRepository;
    private final RestaurantRepository restaurantRepository;

    public Long requireAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request == null ? null : request.getSession(false);
        Object userId = session == null ? null : session.getAttribute("AUTH_USER_ID");
        if (!(userId instanceof Long)) {
            throw new UnauthorizedException("Vui lòng đăng nhập để sử dụng dịch vụ");
        }
        return (Long) userId;
    }

    public Long requireSessionOwner(Long analysisSessionId, HttpServletRequest request) {
        Long userId = requireAuthenticatedUser(request);
        MenuAnalysisSession analysisSession = sessionRepository.findById(analysisSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên phân tích"));
        Restaurant restaurant = restaurantRepository.findById(analysisSession.getRestaurantId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà hàng của phiên phân tích"));
        if (!userId.equals(restaurant.getUserId())) {
            throw new ForbiddenException("Bạn không có quyền truy cập phiên phân tích này");
        }
        return userId;
    }

    public void requireRestaurantOwner(Long restaurantId, Long userId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant không tồn tại"));
        if (!userId.equals(restaurant.getUserId())) {
            throw new ForbiddenException("Bạn không có quyền sử dụng restaurant này");
        }
    }
}
