package com.smartmenu.repository;

import com.smartmenu.entity.GeneratedMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeneratedMenuRepository extends JpaRepository<GeneratedMenu, Long> {
    Optional<GeneratedMenu> findBySessionId(Long sessionId);
}
