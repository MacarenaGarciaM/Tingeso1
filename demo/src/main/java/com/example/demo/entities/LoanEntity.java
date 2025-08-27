package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    private String rutUser;
    private LocalDate reservationDate;
    private LocalDate returnDate;
    private LocalDate lateReturnDate;
    private String nameOfTools;
    private int numberOfTools;
    private int lateFine; //multa por atraso
    private int damagePenalty; //penalización por daños
}
