package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MenuImageAnalysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuImageAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "SessionId", nullable = false)
    private Long sessionId;

    @Column(name = "ExtractedText", columnDefinition = "NVARCHAR(MAX)")
    private String extractedText;

    @Column(name = "LayoutResult", columnDefinition = "NVARCHAR(MAX)")
    private String layoutResult;

    @Column(name = "DesignResult", columnDefinition = "NVARCHAR(MAX)")
    private String designResult;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
