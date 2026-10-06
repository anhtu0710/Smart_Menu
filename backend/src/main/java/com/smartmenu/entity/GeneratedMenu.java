package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "GeneratedMenus")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "SessionId", nullable = false)
    private Long sessionId;

    @Column(name = "GenerationData", columnDefinition = "NVARCHAR(MAX)")
    private String generationData;

    @Column(name = "MenuImageUrl", columnDefinition = "NVARCHAR(MAX)")
    private String menuImageUrl;

    @Column(name = "PdfUrl", columnDefinition = "NVARCHAR(MAX)")
    private String pdfUrl;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
