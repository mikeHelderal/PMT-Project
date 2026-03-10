package com.exercice.pmt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nom;

    private String status ;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String priorite;
    private String statut;
    private LocalDate dateEcheance;
    private LocalDate dateFinReelle;

    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = false)
    @JsonIgnore
    private Project project;

    @ManyToOne
    @JoinColumn(name = "assigne_a_membre_id")
    private ProjectMember assignedMember;
}
