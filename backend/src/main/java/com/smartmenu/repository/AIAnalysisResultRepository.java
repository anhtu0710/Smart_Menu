package com.smartmenu.repository;

import com.smartmenu.entity.AIAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIAnalysisResultRepository extends JpaRepository<AIAnalysisResult, Long> {
    Optional<AIAnalysisResult> findFirstBySessionIdOrderByIdDesc(Long sessionId);
}
