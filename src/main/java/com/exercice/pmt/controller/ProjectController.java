package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.ProjectRequest;
import com.exercice.pmt.model.Project;
import com.exercice.pmt.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des projets : consultation, création et suppression.
 */
@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Tag(name = "Projets", description = "Création et consultation des projets")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Liste les projets auxquels un utilisateur participe, quel que soit son rôle.
     *
     * @param userId identifiant de l'utilisateur
     * @return les projets de l'utilisateur
     */
    @Operation(
            summary = "Lister les projets d'un utilisateur",
            description = "Retourne tous les projets où l'utilisateur est membre, "
                    + "qu'il y soit administrateur, membre ou observateur.")
    @ApiResponse(responseCode = "200", description = "Liste des projets (éventuellement vide)")
    @GetMapping("/{userId}")
    public List<Project> getProjects(
            @Parameter(description = "Identifiant de l'utilisateur", example = "1")
            @PathVariable Long userId){
        return projectService.getAllProjectsByUserId(userId);
    }

    /**
     * Crée un projet ; son créateur en devient automatiquement administrateur.
     *
     * @param project nom, description, date de début et identifiant du créateur
     * @return le projet créé
     */
    @Operation(
            summary = "Créer un projet",
            description = """
                    Crée un projet et inscrit automatiquement son créateur comme
                    administrateur (ligne `membres_projet` avec le rôle ADMIN).

                    Exemple de corps de requête :
                    ```json
                    {
                      "nom": "Refonte du site",
                      "description": "Migration vers Angular et Spring Boot",
                      "dateDebut": "2026-05-01T09:00:00",
                      "adminId": 1
                    }
                    ```
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projet créé"),
            @ApiResponse(responseCode = "404", description = "Utilisateur créateur introuvable")
    })
    @PostMapping
    public Project create(@RequestBody ProjectRequest project){
        return projectService.saveProject(project);
    }

    /**
     * Retourne le détail d'un projet.
     *
     * @param id identifiant du projet
     * @return le projet et ses informations
     */
    @Operation(summary = "Consulter un projet", description = "Retourne le détail d'un projet à partir de son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projet trouvé"),
            @ApiResponse(responseCode = "404", description = "Projet introuvable")
    })
    @GetMapping("/project/{id}")
    public Project getProject(
            @Parameter(description = "Identifiant du projet", example = "1")
            @PathVariable Long id){
        return projectService.getProjectById(id);
    }

    /**
     * Supprime un projet. Réservé à l'administrateur du projet.
     *
     * @param id          identifiant du projet à supprimer
     * @param requesterId identifiant `membres_projet` du demandeur (en-tête X-Member-ID)
     * @return 204 si la suppression a réussi
     */
    @Operation(
            summary = "Supprimer un projet",
            description = "Supprime un projet et ses données rattachées. "
                    + "Réservé à l'administrateur du projet, identifié par l'en-tête `X-Member-ID`.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projet supprimé"),
            @ApiResponse(responseCode = "403", description = "Le demandeur n'est pas administrateur du projet"),
            @ApiResponse(responseCode = "404", description = "Projet introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "Identifiant du projet", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Identifiant `membres_projet` du demandeur", example = "1")
            @RequestHeader("X-Member-ID") Long requesterId) {

        projectService.deleteProject(id, requesterId);
        return ResponseEntity.noContent().build();
    }

}
