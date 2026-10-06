package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AIAnalysisResults")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "SessionId", nullable = false)
    private Long sessionId;

    @Column(name = "Insight", columnDefinition = "NVARCHAR(MAX)")
    private String insight;

    @Column(name = "Recommendation", columnDefinition = "NVARCHAR(MAX)")
    private String recommendation;

    @Column(name = "Strategy", columnDefinition = "NVARCHAR(MAX)")
    private String strategy;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
