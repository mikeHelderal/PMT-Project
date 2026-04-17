package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.ProjectMemberResponse;
import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Role;
import com.exercice.pmt.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur gérant les membres des projets et leurs habilitations.
 * Assure l'invitation, la modification de rôle, et la suppression de membres
 * au sein d'un projet collaboratif.*/
@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProjectMemberController {

    @Autowired
    private final ProjectMemberService memberService;


    /**
     * Récupère la liste de tous les membres associés à un projet spécifique.
     * * @param projectId Identifiant unique du projet
     * @return ResponseEntity contenant la liste des membres formatée (DTO)*/
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectMemberResponse>> getMembers(@PathVariable Integer projectId) {
        return ResponseEntity.ok(memberService.getMembersByProject(projectId));
    }


    /**
     * Invite un nouvel utilisateur dans un projet via son adresse email.
     * * @param projectId Identifiant du projet concerné
     * * @param request Map contenant l'email de l'invité et son futur rôle (roleName)
     * * @param requesterId ID de l'utilisateur effectuant l'invitation (Header X-Member-ID)
     * @return ResponseEntity contenant le membre créé avec un statut 201 (Created)
     */
    @PostMapping("addMember/{projectId}")
    public ResponseEntity<ProjectMember> inviteMember(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-Member-ID") Long requesterId
            )

    {
        String email = request.get("email");
        String roleName = request.get("roleName");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.addMemberByEmail(projectId, email, roleName, requesterId));
    }

    /**
     * Modifie le rôle (permissions) d'un membre existant au sein d'un projet.
     * * @param id Identifiant de la relation ProjectMember à modifier
     * * @param newRole Nouvel objet Role à assigner
     * @return Le membre mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectMember> updateRole(@PathVariable Long id, @RequestBody Role newRole) {
        return ResponseEntity.ok(memberService.updateMemberRole(id, newRole));
    }

    /**
     * Retire un membre d'un projet.
     * * @param id Identifiant unique de l'entrée membre à supprimer
     * * @param requesterId ID de l'utilisateur demandant la suppression pour vérification des droits
     * @return Une réponse 204 No Content en cas de succès
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @RequestHeader("X-Member-ID") Long requesterId) {
        memberService.removeMember(id,requesterId);
        return ResponseEntity.noContent().build();
    }
}
