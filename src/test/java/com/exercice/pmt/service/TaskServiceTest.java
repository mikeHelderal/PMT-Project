package com.exercice.pmt.service;

import com.exercice.pmt.model.*;
import com.exercice.pmt.repository.*;
import jakarta.persistence.EntityNotFoundException;
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
@DisplayName("TaskService - Tests unitaires")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private TaskService taskService;

    private Project project;
    private Role adminRole;
    private Role memberRole;
    private ProjectMember adminMember;
    private ProjectMember regularMember;
    private Task task;

    @BeforeEach
    void setUp() {
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

        task = new Task();
        task.setId(100);
        task.setNom("Tâche test");
        task.setDescription("Description");
        task.setStatus("A_FAIRE");
        task.setProject(project);
    }

    // ---------------------------------------------------------------
    // createTask()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createTask() - succès : tâche créée par un ADMIN")
    void createTask_success_byAdmin() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.createTask(task, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("A_FAIRE");
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("createTask() - statut par défaut 'A_FAIRE' si non renseigné")
    void createTask_defaultStatus_aFaire() {
        task.setStatus(null);
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.createTask(task, 1L);

        assertThat(result.getStatus()).isEqualTo("A_FAIRE");
    }

    @Test
    @DisplayName("createTask() - échec : le demandeur n'est pas ADMIN")
    void createTask_notAdmin_throwsForbidden() {
        when(projectMemberRepository.findById(2L)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> taskService.createTask(task, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Seul l'administrateur peut créer des tâches");
    }

    @Test
    @DisplayName("createTask() - échec : membre demandeur introuvable")
    void createTask_memberNotFound_throwsNotFound() {
        when(projectMemberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(task, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Le membre n'a pas été trouvé");
    }

    @Test
    @DisplayName("createTask() - échec : projet non rattaché à la tâche")
    void createTask_noProject_throwsIllegalArgument() {
        task.setProject(null);
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> taskService.createTask(task, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rattachée à un projet");
    }

    @Test
    @DisplayName("createTask() - échec : projet introuvable en base")
    void createTask_projectNotFound_throwsEntityNotFound() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(projectRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(task, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Projet introuvable");
    }

    // ---------------------------------------------------------------
    // getTasksByProjectId()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getTasksByProjectId() - retourne la liste des tâches du projet")
    void getTasksByProjectId_returnsList() {
        when(taskRepository.findByProjectIdOrderByIdAsc(10)).thenReturn(List.of(task));

        List<Task> result = taskService.getTasksByProjectId(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNom()).isEqualTo("Tâche test");
    }

    // ---------------------------------------------------------------
    // updateStatus()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateStatus() - succès : statut mis à jour")
    void updateStatus_success() {
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateStatus(100, "EN_COURS");

        assertThat(result.getStatus()).isEqualTo("EN_COURS");
        assertThat(result.getDateFinReelle()).isNull();
    }

    @Test
    @DisplayName("updateStatus() - statut TERMINE : dateFinReelle est renseignée")
    void updateStatus_termine_setsDateFinReelle() {
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateStatus(100, "TERMINE");

        assertThat(result.getDateFinReelle()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("updateStatus() - statut non TERMINE : dateFinReelle est remise à null")
    void updateStatus_notTermine_clearsDateFinReelle() {
        task.setDateFinReelle(LocalDate.now());
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateStatus(100, "EN_COURS");

        assertThat(result.getDateFinReelle()).isNull();
    }

    @Test
    @DisplayName("updateStatus() - échec : tâche introuvable")
    void updateStatus_notFound_throwsResponseStatusException() {
        when(taskRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(999, "EN_COURS"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tâche non trouvée");
    }

    // ---------------------------------------------------------------
    // updateTask()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateTask() - succès : nom et description mis à jour")
    void updateTask_success_updatesFields() {
        Task updatedDetails = new Task();
        updatedDetails.setNom("Nouveau nom");
        updatedDetails.setDescription("Nouvelle description");

        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateTask(100, updatedDetails);

        assertThat(result.getNom()).isEqualTo("Nouveau nom");
        assertThat(result.getDescription()).isEqualTo("Nouvelle description");
    }

    @Test
    @DisplayName("updateTask() - passage au statut TERMINE : dateFinReelle renseignée")
    void updateTask_statusTermine_setsDateFinReelle() {
        Task updatedDetails = new Task();
        updatedDetails.setStatus("TERMINE");

        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateTask(100, updatedDetails);

        assertThat(result.getDateFinReelle()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("updateTask() - échec : tâche introuvable")
    void updateTask_notFound_throwsResponseStatusException() {
        when(taskRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(999, new Task()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tâche non trouvée");
    }

    // ---------------------------------------------------------------
    // assignTaskToMember()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("assignTaskToMember() - succès : tâche assignée au membre")
    void assignTaskToMember_success() {
        regularMember.setProject(project);
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findById(2L)).thenReturn(Optional.of(regularMember));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.assignTaskToMember(100, 10, 2);

        assertThat(result.getAssignedMember()).isEqualTo(regularMember);
        verify(taskRepository).save(task);
    }

    @Test
    @DisplayName("assignTaskToMember() - échec : tâche n'appartient pas au projet")
    void assignTaskToMember_wrongProject_throwsBadRequest() {
        Project otherProject = new Project();
        otherProject.setId(99);
        task.setProject(otherProject);

        when(taskRepository.findById(100)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.assignTaskToMember(100, 10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'appartient pas à ce projet");
    }

    @Test
    @DisplayName("assignTaskToMember() - échec : membre n'appartient pas au projet")
    void assignTaskToMember_memberWrongProject_throwsBadRequest() {
        Project otherProject = new Project();
        otherProject.setId(99);
        regularMember.setProject(otherProject);

        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findById(2L)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> taskService.assignTaskToMember(100, 10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'appartient pas à ce projet");
    }

    // ---------------------------------------------------------------
    // deleteTask()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deleteTask() - succès : ADMIN supprime une tâche")
    void deleteTask_success() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(taskRepository.existsById(100)).thenReturn(true);

        assertThatCode(() -> taskService.deleteTask(100, 1L))
                .doesNotThrowAnyException();

        verify(taskRepository).deleteById(100);
    }

    @Test
    @DisplayName("deleteTask() - échec : demandeur non ADMIN")
    void deleteTask_notAdmin_throwsForbidden() {
        when(projectMemberRepository.findById(2L)).thenReturn(Optional.of(regularMember));

        assertThatThrownBy(() -> taskService.deleteTask(100, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Seul l'administrateur peut supprimer une tâche");
    }

    @Test
    @DisplayName("deleteTask() - échec : tâche introuvable")
    void deleteTask_taskNotFound_throwsNotFound() {
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(adminMember));
        when(taskRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(999, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'existe pas");
    }
}
