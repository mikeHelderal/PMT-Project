package com.exercice.pmt.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "membres_projet")
@Data
@NoArgsConstructor
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "date_arrivee", nullable = false)
    private LocalDate dateArrivee;
}
