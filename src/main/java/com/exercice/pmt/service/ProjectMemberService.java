package com.exercice.pmt.service;

import com.exercice.pmt.model.*;
import com.exercice.pmt.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository,
                                ProjectRepository projectRepository,
                                UserRepository userRepository,
                                RoleRepository roleRepository) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<ProjectMember> getMembersByProject(Long projectId) {
        return projectMemberRepository.findByProjectId(Math.toIntExact(projectId));
    }

    @Transactional
    public ProjectMember addMemberByEmail(Long projectId, String email, String roleName) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Projet non trouvé"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Utilisateur non trouvé"));

        Role role = roleRepository.findByLibelle(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Rôle '" + roleName + "' inexistant en base"));

        if (projectMemberRepository.existsByProjectIdAndUserId(Math.toIntExact(projectId), user.getId())) {
            throw new IllegalStateException("L'utilisateur est déjà membre du projet");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);

        return projectMemberRepository.save(member);
    }
}