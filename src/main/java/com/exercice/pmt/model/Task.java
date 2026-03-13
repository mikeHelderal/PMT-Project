package com.exercice.pmt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Data
@Getter
@Setter
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
    @JsonIgnoreProperties({"projects", "password"})
    private User assignee;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String priorite;
    private LocalDate dateEcheance;
    private LocalDate dateFinReelle;

    @ManyToOne
    @JoinColumn(name = "projet_id", nullable = false)
    @JsonIgnoreProperties("tasks")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "assigne_a_membre_id")
    @JsonIgnoreProperties({"project"})
    private ProjectMember assignedMember;
}
