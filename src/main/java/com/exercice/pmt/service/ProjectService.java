package com.exercice.pmt.service;

import com.exercice.pmt.DTO.ProjectRequest;
import com.exercice.pmt.model.Project;
import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Role;
import com.exercice.pmt.model.User;
import com.exercice.pmt.repository.ProjectMemberRepository;
import com.exercice.pmt.repository.ProjectRepository;
import com.exercice.pmt.repository.RoleRepository;
import com.exercice.pmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectMemberRepository projectMemberRepository;


    public List<Project> getAllProjectsByUserId(Long userId) {
        return projectRepository.findByAdminId(userId);
    }

    public Project saveProject(ProjectRequest project) {
        User admin = userRepository.findById(project.getAdminId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Project newProject = new Project();
        newProject.setNom(project.getNom());
        newProject.setDescription(project.getDescription());
        newProject.setDateDebut(project.getDateDebut());
        newProject.setAdmin(admin);



        Project savedProject =  projectRepository.save(newProject);

        Role adminRole = roleRepository.findByLibelle("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rôle ADMIN introuvable"));

        ProjectMember projectMember = new ProjectMember();
        projectMember.setProject(savedProject);
        projectMember.setUser(admin);
        projectMember.setRole(adminRole);
        projectMember.setDateArrivee(LocalDate.now());

        projectMemberRepository.save(projectMember);

        return savedProject;


    }

    @Transactional
    public void deleteProject(Long id, Long requesterId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!project.getAdmin().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé");
        }
        projectRepository.deleteById(id);
    }

    public Project getProjectById(Long id){
        return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}
