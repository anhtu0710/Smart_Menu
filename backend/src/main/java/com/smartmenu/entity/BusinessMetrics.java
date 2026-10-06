package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "BusinessMetrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "SessionId", nullable = false)
    private Long sessionId;

    @Column(name = "TotalRevenue", precision = 18, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "TotalQuantity")
    private Integer totalQuantity;

    @Column(name = "EstimatedProfit", precision = 18, scale = 2)
    private BigDecimal estimatedProfit;

    @Column(name = "MenuScore")
    private Integer menuScore;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
