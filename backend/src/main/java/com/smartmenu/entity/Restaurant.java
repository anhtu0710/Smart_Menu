package com.smartmenu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Restaurants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "UserId", nullable = false)
    private Long userId;

    @Column(name = "RestaurantName", length = 255)
    private String restaurantName;

    @Column(name = "BusinessType", length = 100)
    private String businessType;

    @Column(name = "Address", columnDefinition = "NVARCHAR(MAX)")
    private String address;

    @Builder.Default
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt = LocalDateTime.now();
}
