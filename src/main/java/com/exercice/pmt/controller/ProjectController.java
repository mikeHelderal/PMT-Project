package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.ProjectRequest;
import com.exercice.pmt.model.Project;
import com.exercice.pmt.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST assurant la gestion du cycle de vie des projets.
 * Permet la création, la consultation et la suppression sécurisée de projets
 * liés à des utilisateurs spécifique*/
@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;


    /**
     * Récupère la liste de tous les projets associés à un utilisateur donné.
     * * @param userId Identifiant unique de l'utilisateur
     * @return Une liste d'objets Project
     */
    @GetMapping("/{userId}")
    public List<Project> getProjects(@PathVariable Long userId){
        return projectService.getAllProjectsByUserId(userId);
    }

    /**
     * Crée un nouveau projet dans le système.
     * * @param project Objet DTO contenant les informations du projet à créer
     * @return Le projet enregistré avec son identifiant généré
     */
    @PostMapping
    public Project create(@RequestBody ProjectRequest project){
        return projectService.saveProject(project);
    }

    /**
     * Récupère les détails d'un projet spécifique par son identifiant.
     * * @param id Identifiant unique du projet
     * @return  L'objet Project correspondant
     */
    @GetMapping("/project/{id}")
    public Project getProject(@PathVariable Long id){
        return projectService.getProjectById(id);
    }

    /**
     * Supprime un projet après vérification des droits de l'utilisateur.
     * * @param id Identifiant du projet à supprimer
     * * @param requesterId Idnetifiant de l'utilisateur demandant la suppression (via Header X-Member-ID)
     * @return Une réponse 204 No content en cas de succès
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @RequestHeader("X-Member-ID") Long requesterId) { // Ajout du header de sécurité

        projectService.deleteProject(id, requesterId);
        return ResponseEntity.noContent().build();
    }

}
