package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    // Cliente
    private String rutUser;

    // Fechas
    private LocalDate reservationDate;   // fecha de entrega
    private LocalDate returnDate;        // fecha pactada de devolución
    private LocalDate lateReturnDate;    // fecha real de devolución (null = activo)

    // Totales / multas
    private int total;           // cuánto debe pagar
    private int lateFine;        // multa por atraso
    private int damagePenalty;   // penalización por daños

    // Cantidad de TIPOS de herramientas en el préstamo (tamaño de items)
    private Integer amountOfTools = 0;

    // Ítems (líneas) del préstamo
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanItemEntity> items = new ArrayList<>();

    // Helper para mantener amountOfTools en sincronía
    public void addItem(LoanItemEntity item) {
        items.add(item);
        item.setLoan(this);
        amountOfTools = items.size();
    }

    public void removeItem(LoanItemEntity item) {
        items.remove(item);
        item.setLoan(null);
        amountOfTools = items.size();
    }
}
