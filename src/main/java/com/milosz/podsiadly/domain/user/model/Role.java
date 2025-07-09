package com.milosz.podsiadly.domain.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false, unique = true)
    private UserRole name; // Using UserRole enum for role names

    public enum UserRole {
        CUSTOMER,
        EMPLOYEE,
        ADMIN,
        ANALYST,
        AUDITOR,
        SIMULATION_USER
    }
}