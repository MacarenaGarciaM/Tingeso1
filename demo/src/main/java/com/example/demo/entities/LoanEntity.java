// src/main/java/com/example/demo/entities/LoanEntity.java
package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    private Long id;

    // Cliente
    private String rutUser;

    // Fechas
    private LocalDate reservationDate;   // fecha de entrega
    private LocalDate returnDate;        // fecha pactada devolución
    private LocalDate lateReturnDate;    // fecha real devolución (null=activo)

    // Monto base arriendo (se calcula al crear)
    private int total = 0;

    // Multas/penalizaciones
    private int lateFine = 0;            // multa por atraso
    private int damagePenalty = 0;       // penalización por daños

    // NUEVO: estado de pago de multas
    private boolean lateFinePaid = false;
    private boolean damagePenaltyPaid = false;

    // Cantidad de TIPOS de herramientas en el préstamo (tamaño de items)
    private Integer amountOfTools = 0;

    // Ítems del préstamo
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<LoanItemEntity> items = new ArrayList<>();

    // Helpers
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
