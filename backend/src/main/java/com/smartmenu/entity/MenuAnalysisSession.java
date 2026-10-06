package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "MenuAnalysisSessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuAnalysisSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "RestaurantId", nullable = false)
    private Long restaurantId;

    /**
     * Workflow status:
     *
     * UPLOADED
     * PROCESSING
     * DATA_ANALYZED
     * AI_ANALYZING
     * COMPLETED
     * FAILED
     */
    @Builder.Default
    @Column(name = "Status", length = 50)
    private String status = "UPLOADED";

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();

}