package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ProcessedBusinessData")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedBusinessData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "SessionId", nullable = false)
    private Long sessionId;

    @Column(name = "AnalysisPayload", columnDefinition = "NVARCHAR(MAX)")
    private String analysisPayload;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
