package com.smartmenu.repository;

import com.smartmenu.entity.DataMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataMappingRepository extends JpaRepository<DataMapping, Long> {
    List<DataMapping> findBySessionId(Long sessionId);
}
