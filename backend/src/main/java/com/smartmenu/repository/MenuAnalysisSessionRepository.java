package com.smartmenu.repository;

import com.smartmenu.entity.MenuAnalysisSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuAnalysisSessionRepository extends JpaRepository<MenuAnalysisSession, Long> {
    List<MenuAnalysisSession> findByRestaurantId(Long restaurantId);
}
