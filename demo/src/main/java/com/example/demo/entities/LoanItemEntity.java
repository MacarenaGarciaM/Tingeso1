package com.example.demo.entities;

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
    private LoanEntity loan;

    // Herramienta (referencia)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    private ToolEntity tool;

    // Snapshot del nombre (opcional pero útil si el nombre cambia)
    private String toolNameSnapshot;

    // Si tu regla es "no más de una unidad de la misma herramienta a la vez",
    // deja quantity = 1 siempre. Si más adelante permites >1, ajusta validaciones.
    private Integer quantity = 1;
}
