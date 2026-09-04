package com.exercice.pmt.service;

import com.exercice.pmt.DTO.ProjectRequest;
import com.exercice.pmt.model.*;
import com.exercice.pmt.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService - Tests unitaires")
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectService projectService;

    private User admin;
    private Project project;
    private Role adminRole;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin_user");
        admin.setEmail("admin@example.com");

        project = new Project();
        project.setId(10);
        project.setNom("Mon Projet");
        project.setDescription("Description du projet");
        project.setDateDebut(LocalDate.of(2024, 1, 1).atStartOfDay());
        project.setAdmin(admin);

        adminRole = new Role();
        adminRole.setLibelle("ADMIN");

        projectRequest = new ProjectRequest();
        projectRequest.setAdminId(1);
        projectRequest.setNom("Mon Projet");
        projectRequest.setDescription("Description du projet");
        projectRequest.setDateDebut(LocalDate.of(2024, 1, 1).atStartOfDay());
    }

    // ---------------------------------------------------------------
    // getAllProjectsByUserId()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getAllProjectsByUserId() - retourne la liste des projets de l'utilisateur")
    void getAllProjectsByUserId_returnsList() {
        when(projectRepository.findByAdminId(1L)).thenReturn(List.of(project));

        List<Project> result = projectService.getAllProjectsByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Mon Projet");
    }

    @Test
    @DisplayName("getAllProjectsByUserId() - retourne une liste vide si aucun projet")
    void getAllProjectsByUserId_returnsEmptyList() {
        when(projectRepository.findByAdminId(99L)).thenReturn(List.of());

        List<Project> result = projectService.getAllProjectsByUserId(99L);

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // saveProject()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("saveProject() - succès : projet créé et admin ajouté comme membre")
    void saveProject_success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(roleRepository.findByLibelle("ADMIN")).thenReturn(Optional.of(adminRole));
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());

        Project result = projectService.saveProject(projectRequest);

        assertThat(result).isNotNull();
        assertThat(result.getNom()).isEqualTo("Mon Projet");
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("saveProject() - échec : utilisateur admin introuvable")
    void saveProject_adminNotFound_throwsRuntimeException() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.saveProject(projectRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utilisateur introuvable");

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveProject() - échec : rôle ADMIN introuvable")
    void saveProject_adminRoleNotFound_throwsRuntimeException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(roleRepository.findByLibelle("ADMIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.saveProject(projectRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Rôle ADMIN introuvable");
    }

    // ---------------------------------------------------------------
    // deleteProject()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deleteProject() - succès : admin supprime son projet")
    void deleteProject_success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatCode(() -> projectService.deleteProject(10L, 1L))
                .doesNotThrowAnyException();

        verify(projectRepository).deleteById(10L);
    }

    @Test
    @DisplayName("deleteProject() - échec : projet introuvable")
    void deleteProject_notFound_throwsResponseStatusException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(99L, 1L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("deleteProject() - échec : utilisateur non admin du projet")
    void deleteProject_notAdmin_throwsResponseStatusException() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.deleteProject(10L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Non autorisé");
    }

    // ---------------------------------------------------------------
    // getProjectById()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getProjectById() - succès : projet retourné")
    void getProjectById_success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        Project result = projectService.getProjectById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
    }

    @Test
    @DisplayName("getProjectById() - échec : projet introuvable")
    void getProjectById_notFound_throwsRuntimeException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utilisateur introuvable");
    }
}
