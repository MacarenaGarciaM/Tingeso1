package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "client",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_client_keycloak", columnNames = "keycloak_id"),
                @UniqueConstraint(name = "uk_client_email",    columnNames = "email"),
                @UniqueConstraint(name = "uk_client_rut",      columnNames = "rut")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    /** sub de Keycloak (UUID del usuario en el IdP) */
    @Column(name = "keycloak_id", nullable = false, length = 64)
    private String keycloakId;

    private String name;

    @Column(nullable = false)
    private String email;

    /** Ya no usamos password local (ver Security) */
    @Transient
    private String password;
    @Column(name = "rut", unique = true)
    private String rut;

    private int phone;
    private boolean admin;
    private boolean active;
    private int amountOfLoans;
}
