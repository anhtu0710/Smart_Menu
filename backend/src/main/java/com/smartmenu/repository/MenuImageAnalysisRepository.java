package com.smartmenu.repository;

import com.smartmenu.entity.MenuImageAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MenuImageAnalysisRepository extends JpaRepository<MenuImageAnalysis, Long> {
    Optional<MenuImageAnalysis> findBySessionId(Long sessionId);
}
