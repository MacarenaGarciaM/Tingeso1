// src/main/java/com/example/demo/entities/SettingEntity.java
package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_setting")
@Data @NoArgsConstructor @AllArgsConstructor
public class SettingEntity {
    @Id
    @Column(length = 100)
    private String price;          // p.ej. "daily_rent_price"

    @Column(nullable = false)
    private String text;          // guardamos como texto para simplicidad
}
