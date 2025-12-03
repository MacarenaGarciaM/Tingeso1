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

    // Client
    private String rutUser;

    // Dates
    private LocalDate reservationDate;
    private LocalDate returnDate;
    private LocalDate lateReturnDate;

    // first amount of rent
    private int total = 0;

    //fines
    private int lateFine = 0;
    private int damagePenalty = 0;

    // state of fine payments
    private boolean lateFinePaid = false;
    private boolean damagePenaltyPaid = false;

    //Amount of types of tools in the loan
    private Integer amountOfTools = 0;

    // Items of the loan
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
