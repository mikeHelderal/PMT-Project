package com.exercice.pmt.service;

import com.exercice.pmt.DTO.ProjectMemberResponse;
import com.exercice.pmt.model.*;
import com.exercice.pmt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Service gérant les membres et les habilitations au sein des projets.
 * Ce service implémente les règles métier liées à la collaboration :
 * vérification des droits d'administration, unicité des membres et gestion des rôles.
 */
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Récupère la liste des membres d'un projet formatée pour la vue (DTO).
     * * @param projectId Identifiant du projet
     * @return Liste de ProjectMemberResponse contenant les informations essentielles des membres
     */
    public List<ProjectMemberResponse> getMembersByProject(Integer projectId) {
        return projectMemberRepository.findByProjectId(projectId)
                .stream()
                .map(member -> new ProjectMemberResponse(
                        member.getId(),
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getUser().getEmail(),
                        member.getRole().getLibelle(),
                        member.getDateArrivee()
                ))
                .toList();
    }

    /**
     * Ajoute un membre à un projet via son adresse email.
     * Cette opération est transactionnelle et vérifie les droits de l'appelant.
     * * @param projectId Identifiant du projet cible
     * * @param email Email de l'utilisateur à inviter
     * * @param roleName Nom du rôle à attribuer (ex: ADMIN, MEMBER)
     * * @param memberId ID du membre effectuant l'ajout (doit être ADMIN)
     * @return Le nouveau ProjectMember enregistré
     * * @throws ResponseStatusException 403 si l'appelant n'est pas ADMIN
     * * @throws ResponseStatusException 404 si le projet, l'utilisateur ou le rôle n'existe pas
     * * @throws ResponseStatusException 409 si l'utilisateur est déjà présent dans le projet
     */
    @Transactional
    public ProjectMember addMemberByEmail(Long projectId, String email, String roleName, Long memberId) {

        ProjectMember requesterMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Membre non trouvé"));

        if(!"ADMIN".equalsIgnoreCase(requesterMember.getRole().getLibelle())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seul l'admin peut ajouter un membre");
        }



        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet non trouvé"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur avec l'email " + email + " non trouvé"));

        Role role = roleRepository.findByLibelle(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle '" + roleName + "' inexistant"));

        if (projectMemberRepository.existsByProjectIdAndUserId(Math.toIntExact(projectId), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utilisateur est déjà membre du projet");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        member.setDateArrivee(LocalDate.now());

        return projectMemberRepository.save(member);
    }

    /**
     * Supprime un membre d'un projet.
     * * @param memberId Identifiant de la relation membre à supprimer
     * * @param requesterMemberID ID de l'appelant (doit être ADMIN)
     * * @throws ResponseStatusException 403 si l'appelant n'a pas les droits
     */
    @Transactional
    public void removeMember(Long memberId, Long requesterMemberID) {

        ProjectMember requesterMember = projectMemberRepository.findById(requesterMemberID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Membre non trouvé"));

        if(!"ADMIN".equalsIgnoreCase(requesterMember.getRole().getLibelle())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seul l'admin peut ajouter un membre");
        }
        if (!projectMemberRepository.existsById(memberId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé");
        }
        projectMemberRepository.deleteById(memberId);
    }

    /**
     * Modifie le rôle d'un membre.
     * *@param id Identifiant de la relation membre
     * *@param newRole Nouvel objet Role à affecter
     * @return Le membre mis à jour
     */
    @Transactional
    public ProjectMember updateMemberRole(Long id, Role newRole) {
        ProjectMember member = projectMemberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));

        member.setRole(newRole);
        return projectMemberRepository.save(member);
    }
}