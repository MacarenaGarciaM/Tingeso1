package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "loan_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"loan_id","tool_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cabecera
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    @JsonBackReference
    private LoanEntity loan;

    // Herramienta (referencia)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    @JsonIgnoreProperties({"items"})
    private ToolEntity tool;

    // Snapshot del nombre (opcional pero útil si el nombre cambia)
    private String toolNameSnapshot;

}
