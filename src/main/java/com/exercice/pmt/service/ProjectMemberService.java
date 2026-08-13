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


@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
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
     * Modifie le rôle d'un membre sur un projet.
     * <p>
     * Réservé à l'administrateur : sans ce contrôle, n'importe quel appelant
     * pourrait se promouvoir administrateur du projet.
     *
     * @param id                identifiant du membre dont le rôle change
     * @param newRole           rôle demandé ; seul son libellé est retenu, il est
     *                          systématiquement re-résolu en base
     * @param requesterMemberID identifiant du membre à l'origine de la requête (X-Member-ID)
     * @throws ResponseStatusException 403 si l'appelant n'est pas ADMIN,
     *                                 404 si le membre ou le rôle est introuvable
     */
    @Transactional
    public ProjectMember updateMemberRole(Long id, Role newRole, Long requesterMemberID) {

        assertRequesterIsAdmin(requesterMemberID, "modifier le rôle d'un membre");

        ProjectMember member = projectMemberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));

        if (newRole == null || newRole.getLibelle() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle est obligatoire");
        }

        // Le rôle est re-résolu en base : on n'enregistre jamais un libellé arbitraire
        Role role = roleRepository.findByLibelle(newRole.getLibelle())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Rôle '" + newRole.getLibelle() + "' inexistant"));

        member.setRole(role);
        return projectMemberRepository.save(member);
    }

    /**
     * Vérifie que le membre à l'origine de la requête est administrateur du projet.
     *
     * @param requesterMemberID identifiant `membres_projet` de l'appelant (X-Member-ID)
     * @param action            libellé de l'action, repris dans le message d'erreur
     */
    private void assertRequesterIsAdmin(Long requesterMemberID, String action) {
        ProjectMember requester = projectMemberRepository.findById(requesterMemberID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Membre non trouvé"));

        if (!"ADMIN".equalsIgnoreCase(requester.getRole().getLibelle())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Seul l'administrateur peut " + action);
        }
    }
}