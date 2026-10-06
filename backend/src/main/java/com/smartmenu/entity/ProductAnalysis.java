package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ProductAnalysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "SessionId", nullable = false)
    private Long sessionId;

    @Column(name = "ProductName", columnDefinition = "NVARCHAR(MAX)")
    private String productName;

    @Column(name = "AnalysisType", length = 50)
    private String analysisType;

    @Column(name = "Reason", columnDefinition = "NVARCHAR(MAX)")
    private String reason;

    @Column(name = "Metrics", columnDefinition = "NVARCHAR(MAX)")
    private String metrics;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
