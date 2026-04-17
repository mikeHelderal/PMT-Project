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

/**
 * Service gérant la logique métier relative aux projets.
 * Assure la création des projets, la gestion de leur cycle de vie et
 * l'attribution automatique des rôles d'administration aux créateurs.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectMemberRepository projectMemberRepository;


    /**
     * Récupère la liste de tous les projets dont l'utilisateur est l'administrateur.
     * * @param userId Identifiant de l'utilisateur
     * @return Liste des projets administrés par l'utilisateur
     */
    public List<Project> getAllProjectsByUserId(Long userId) {
        return projectRepository.findByAdminId(userId);
    }

    /**
     * Crée un nouveau projet et définit l'utilisateur créateur comme ADMIN du projet.
     * Cette méthode orchestre la création de l'entité Project et de l'entité ProjectMember.
     * * @param project DTO contenant les informations du projet et l'ID de l'administrateur
     * @return Le projet nouvellement créé
     * * @throws RuntimeException si l'utilisateur ou le rôle ADMIN est introuvable en base
     */
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

    /**
     * Supprime un projet après vérification que le demandeur est bien l'administrateur.
     * L'annotation @Transactional garantit que la suppression est atomique.
     * @param id Identifiant du projet à supprimer
     * @param requesterId Identifiant de l'utilisateur demandant la suppression
     * @throws ResponseStatusException 404 si le projet n'existe pas
     * @throws ResponseStatusException 403 si l'utilisateur n'est pas l'administrateur du projet
     */
    @Transactional
    public void deleteProject(Long id, Long requesterId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!project.getAdmin().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé");
        }
        projectRepository.deleteById(id);
    }

    /**
     * Récupère les informations détaillées d'un projet par son ID.
     * * @param id Identifiant du projet
     * @return L'entité Project
     * * @throws RuntimeException si le projet est introuvable
     */
    public Project getProjectById(Long id){
        return projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}
