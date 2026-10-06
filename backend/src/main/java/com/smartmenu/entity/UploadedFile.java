package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "UploadedFiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "SessionId", nullable = false)
    private Long sessionId;

    @Column(name = "FileName", columnDefinition = "NVARCHAR(MAX)")
    private String fileName;

    @Column(name = "FilePath", columnDefinition = "NVARCHAR(MAX)")
    private String filePath;

    @Column(name = "FileType")
    private String fileType;

    /**
     * BUSINESS_EXCEL
     *
     * OLD_MENU_IMAGE
     */
    @Column(name = "FileCategory", length = 50)
    private String fileCategory;

    @Column(name = "FileSize")
    private Long fileSize;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();

}