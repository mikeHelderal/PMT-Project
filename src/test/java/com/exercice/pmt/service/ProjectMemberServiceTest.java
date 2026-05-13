package com.exercice.pmt.service;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMemberService - Tests unitaires")
class ProjectMemberServiceTest {

    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private User user;
    private Project project;
    private Role adminRole;
    private Role memberRole;
    private ProjectMember adminMember;
    private ProjectMember regularMember;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(2L);
        user.setUsername("jane");
        user.setEmail("jane@example.com");

        project = new Project();
        project.setId(10);

        adminRole = new Role();
        adminRole.setLibelle("ADMIN");

        memberRole = new Role();
        memberRole.setLibelle("MEMBRE");

        adminMember = new ProjectMember();
        adminMember.setId(1);
        adminMember.setRole(adminRole);
        adminMember.setProject(project);

        regularMember = new ProjectMember();
        regularMember.setId(2);
        regularMember.setRole(memberRole);
        regularMember.setProject(project);
        regularMember.setUser(user);
    }

    // ---------------------------------------------------------------
    // addMemberByEmail()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("addMemberByEmail() - succès : admin ajoute un nouveau membre")
    void addMemberByEmail_success() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByLibelle("MEMBRE")).thenReturn(Optional.of(memberRole));
        when(projectMemberRepository.existsByProjectIdAndUserId(10, 2L)).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(regularMember);

        ProjectMember result = projectMemberService.addMemberByEmail(10L, "jane@example.com", "MEMBRE", 1L);

        assertThat(result).isNotNull();
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("addMemberByEmail() - échec : le demandeur n'est pas ADMIN")
    void addMemberByEmail_requesterNotAdmin_throwsForbidden() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() ->
                projectMemberService.addMemberByEmail(10L, "jane@example.com", "MEMBRE", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Seul l'admin peut ajouter un membre");
    }

    @Test
    @DisplayName("addMemberByEmail() - échec : demandeur introuvable")
    void addMemberByEmail_requesterNotFound_throwsForbidden() {
        when(projectMemberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                projectMemberService.addMemberByEmail(10L, "jane@example.com", "MEMBRE", 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Membre non trouvé");
    }

    @Test
    @DisplayName("addMemberByEmail() - échec : projet introuvable")
    void addMemberByEmail_projectNotFound_throwsNotFound() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                projectMemberService.addMemberByEmail(99L, "jane@example.com", "MEMBRE", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Projet non trouvé");
    }

    @Test
    @DisplayName("addMemberByEmail() - échec : utilisateur introuvable par email")
    void addMemberByEmail_userNotFound_throwsNotFound() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                projectMemberService.addMemberByEmail(10L, "unknown@example.com", "MEMBRE", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non trouvé");
    }

    @Test
    @DisplayName("addMemberByEmail() - échec : utilisateur déjà membre du projet")
    void addMemberByEmail_alreadyMember_throwsConflict() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(roleRepository.findByLibelle("MEMBRE")).thenReturn(Optional.of(memberRole));
        when(projectMemberRepository.existsByProjectIdAndUserId(10, 2L)).thenReturn(true);

        assertThatThrownBy(() ->
                projectMemberService.addMemberByEmail(10L, "jane@example.com", "MEMBRE", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("déjà membre");
    }

    // ---------------------------------------------------------------
    // removeMember()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("removeMember() - succès : admin supprime un membre")
    void removeMember_success() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectMemberRepository.existsById(2L)).thenReturn(true);

        assertThatCode(() -> projectMemberService.removeMember(2L, 1L))
                .doesNotThrowAnyException();

        verify(projectMemberRepository).deleteById(2L);
    }

    @Test
    @DisplayName("removeMember() - échec : demandeur non ADMIN")
    void removeMember_requesterNotAdmin_throwsForbidden() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> projectMemberService.removeMember(2L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Seul l'admin");
    }

    @Test
    @DisplayName("removeMember() - échec : membre à supprimer introuvable")
    void removeMember_memberNotFound_throwsNotFound() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectMemberRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.removeMember(99L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Membre non trouvé");
    }

    // ---------------------------------------------------------------
    // updateMemberRole()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateMemberRole() - succès : rôle mis à jour")
    void updateMemberRole_success() {
        when(projectMemberRepository.findById(2L)).thenReturn(Optional.of(regularMember));
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(regularMember);

        ProjectMember result = projectMemberService.updateMemberRole(2L, adminRole);

        assertThat(result).isNotNull();
        verify(projectMemberRepository).save(regularMember);
    }

    @Test
    @DisplayName("updateMemberRole() - échec : membre introuvable")
    void updateMemberRole_notFound_throwsNotFound() {
        when(projectMemberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(99L, adminRole))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Membre introuvable");
    }
}
