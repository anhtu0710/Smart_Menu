package com.smartmenu.repository;

import com.smartmenu.entity.MenuGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuGenerationRepository extends JpaRepository<MenuGeneration, Long> {

    List<MenuGeneration> findByUserIdOrderByCreatedDateDesc(Long userId);

    List<MenuGeneration> findBySessionId(Long sessionId);
}
