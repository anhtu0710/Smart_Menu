package com.smartmenu.repository;

import com.smartmenu.entity.ProcessedBusinessData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedBusinessDataRepository extends JpaRepository<ProcessedBusinessData, Long> {
    Optional<ProcessedBusinessData> findBySessionId(Long sessionId);
}
