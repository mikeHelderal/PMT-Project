package com.exercice.pmt.service;

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
    public List<ProjectMember> getMembersByProject(Integer projectId) {
        return projectMemberRepository.findByProjectId(projectId);
    }

    @Transactional
    public ProjectMember addMemberByEmail(Long projectId, String email, String roleName) {
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
    public void removeMember(Long memberId) {
        if (!projectMemberRepository.existsById(memberId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé");
        }
        projectMemberRepository.deleteById(memberId);
    }

    @Transactional
    public ProjectMember updateMemberRole(Long id, Role newRole) {
        ProjectMember member = projectMemberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));

        member.setRole(newRole);
        return projectMemberRepository.save(member);
    }
}