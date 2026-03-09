package com.exercice.pmt.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_taches")
@Data
@NoArgsConstructor
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "tache_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "auteur_membre_id", nullable = false)
    private ProjectMember authorMember;

    private String action;
    private LocalDateTime dateAction;

    @PrePersist
    protected void onCreate() {
        dateAction = LocalDateTime.now();
    }
}
