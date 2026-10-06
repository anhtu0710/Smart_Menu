package com.smartmenu.repository;

import com.smartmenu.entity.RawExcelData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawExcelDataRepository extends JpaRepository<RawExcelData, Long> {
    List<RawExcelData> findByFileId(Long fileId);
}
