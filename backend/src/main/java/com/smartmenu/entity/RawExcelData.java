package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "RawExcelData")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawExcelData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "FileId", nullable = false)
    private Long fileId;

    @Column(name = "SheetName", columnDefinition = "NVARCHAR(MAX)")
    private String sheetName;

    @Column(name = "RowNumber")
    private Integer rowNumber;

    @Column(name = "ColumnName", columnDefinition = "NVARCHAR(MAX)")
    private String columnName;

    @Column(name = "ColumnValue", columnDefinition = "NVARCHAR(MAX)")
    private String columnValue;

    /**
     * TEXT
     * NUMBER
     * DATE
     * CURRENCY
     */
    @Column(name = "DataType", length = 50)
    private String dataType;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

}