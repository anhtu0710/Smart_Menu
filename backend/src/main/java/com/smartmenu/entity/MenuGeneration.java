package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MENU_GENERATION")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "generation_type", nullable = false, length = 20)
    private String generationType; // 'FREE' hoặc 'PAID'

    @Column(name = "style_id", length = 50)
    private String styleId;

    @Column(name = "menu_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String menuImageUrl;

    @Column(name = "created_date")
    @Builder.Default
    private LocalDateTime createdDate = LocalDateTime.now();
}
