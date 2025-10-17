package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "kardex")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KardexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private ToolEntity tool;
    private String rutUser;
    private String type;
    private LocalDate movementDate;
    private int stock;
}
