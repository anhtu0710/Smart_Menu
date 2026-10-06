package com.smartmenu.repository;

import com.smartmenu.entity.BusinessMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessMetricsRepository extends JpaRepository<BusinessMetrics, Long> {
    Optional<BusinessMetrics> findBySessionId(Long sessionId);
}
