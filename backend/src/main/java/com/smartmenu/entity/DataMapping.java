package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "DataMappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "SessionId", nullable = false)
    private Long sessionId;

    @Column(name = "OriginalColumn", columnDefinition = "NVARCHAR(MAX)")
    private String originalColumn;

    @Column(name = "MappedField", length = 100)
    private String mappedField;

    @Column(name = "Confidence")
    private Double confidence;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
