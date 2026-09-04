package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.ProjectMemberResponse;
import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Role;
import com.exercice.pmt.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gestion des membres d'un projet et de leurs rôles.
 */
@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Tag(name = "Membres de projet", description = "Invitation des membres et attribution des rôles")
public class ProjectMemberController {

    @Autowired
    private final ProjectMemberService memberService;

    /**
     * Liste les membres d'un projet avec leur rôle.
     *
     * @param projectId identifiant du projet
     * @return les membres du projet
     */
    @Operation(
            summary = "Lister les membres d'un projet",
            description = "Retourne les membres du projet avec leur rôle (ADMIN, MEMBER, GUEST) "
                    + "et leur date d'arrivée.")
    @ApiResponse(responseCode = "200", description = "Liste des membres")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectMemberResponse>> getMembers(
            @Parameter(description = "Identifiant du projet", example = "1")
            @PathVariable Integer projectId) {
        return ResponseEntity.ok(memberService.getMembersByProject(projectId));
    }

    /**
     * Invite un utilisateur existant à rejoindre un projet, avec un rôle donné.
     *
     * @param projectId   identifiant du projet
     * @param request     corps contenant `email` et `roleName`
     * @param requesterId identifiant `membres_projet` du demandeur (en-tête X-Member-ID)
     * @return le membre créé
     */
    @Operation(
            summary = "Inviter un membre par e-mail",
            description = """
                    Ajoute au projet un utilisateur déjà inscrit, identifié par son adresse
                    e-mail, et lui attribue un rôle. Réservé à l'administrateur du projet.

                    Exemple de corps de requête :
                    ```json
                    { "email": "paul@pmt.com", "roleName": "MEMBER" }
                    ```
                    Rôles acceptés : `ADMIN`, `MEMBER`, `GUEST`.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Membre ajouté au projet"),
            @ApiResponse(responseCode = "403", description = "Le demandeur n'est pas administrateur"),
            @ApiResponse(responseCode = "404", description = "Projet, utilisateur ou rôle introuvable"),
            @ApiResponse(responseCode = "409", description = "L'utilisateur est déjà membre du projet")
    })
    @PostMapping("addMember/{projectId}")
    public ResponseEntity<ProjectMember> inviteMember(
            @Parameter(description = "Identifiant du projet", example = "1")
            @PathVariable Long projectId,
            @RequestBody Map<String, String> request,
            @Parameter(description = "Identifiant `membres_projet` du demandeur (doit être ADMIN)", example = "1")
            @RequestHeader("X-Member-ID") Long requesterId
            )

    {
        String email = request.get("email");
        String roleName = request.get("roleName");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.addMemberByEmail(projectId, email, roleName, requesterId));
    }

    /**
     * Change le rôle d'un membre du projet.
     *
     * @param id          identifiant du membre dont le rôle change
     * @param newRole     rôle demandé ; seul son libellé est pris en compte
     * @param requesterId identifiant `membres_projet` du demandeur (en-tête X-Member-ID)
     * @return le membre avec son nouveau rôle
     */
    @Operation(
            summary = "Modifier le rôle d'un membre",
            description = """
                    Attribue un nouveau rôle à un membre du projet.
                    Réservé à l'administrateur : sans ce contrôle, n'importe quel membre
                    pourrait se promouvoir administrateur.

                    Exemple de corps de requête :
                    ```json
                    { "libelle": "MEMBER" }
                    ```
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôle mis à jour"),
            @ApiResponse(responseCode = "400", description = "Rôle absent du corps de la requête"),
            @ApiResponse(responseCode = "403", description = "Le demandeur n'est pas administrateur"),
            @ApiResponse(responseCode = "404", description = "Membre ou rôle introuvable")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProjectMember> updateRole(
            @Parameter(description = "Identifiant du membre à modifier", example = "2")
            @PathVariable Long id,
            @RequestBody Role newRole,
            @Parameter(description = "Identifiant `membres_projet` du demandeur (doit être ADMIN)", example = "1")
            @RequestHeader("X-Member-ID") Long requesterId) {
        return ResponseEntity.ok(memberService.updateMemberRole(id, newRole, requesterId));
    }

    /**
     * Retire un membre du projet.
     *
     * @param id          identifiant du membre à retirer
     * @param requesterId identifiant `membres_projet` du demandeur (en-tête X-Member-ID)
     * @return 204 si le retrait a réussi
     */
    @Operation(
            summary = "Retirer un membre du projet",
            description = "Supprime la participation d'un membre au projet. "
                    + "Réservé à l'administrateur du projet.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membre retiré"),
            @ApiResponse(responseCode = "403", description = "Le demandeur n'est pas administrateur"),
            @ApiResponse(responseCode = "404", description = "Membre introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Identifiant du membre à retirer", example = "2")
            @PathVariable Long id,
            @Parameter(description = "Identifiant `membres_projet` du demandeur (doit être ADMIN)", example = "1")
            @RequestHeader("X-Member-ID") Long requesterId) {
        memberService.removeMember(id,requesterId);
        return ResponseEntity.noContent().build();
    }
}
