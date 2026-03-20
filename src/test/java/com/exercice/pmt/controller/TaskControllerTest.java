package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.AssignTaskRequest;
import com.exercice.pmt.model.*;
import com.exercice.pmt.repository.ProjectMemberRepository;
import com.exercice.pmt.service.NotificationService;
import com.exercice.pmt.service.TaskHistoryService;
import com.exercice.pmt.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@DisplayName("TaskController - Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;
    @MockitoBean private TaskHistoryService taskHistoryService;
    @MockitoBean private ProjectMemberRepository projectMemberRepository;
    @MockitoBean private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private Task task;
    private ProjectMember member;
    private User user;

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setId(10);

        Role role = new Role();
        role.setLibelle("ADMIN");

        user = new User();
        user.setId(1L);
        user.setUsername("admin_user");
        user.setEmail("admin@example.com");

        member = new ProjectMember();
        member.setId(1);
        member.setRole(role);
        member.setProject(project);
        member.setUser(user);

        task = new Task();
        task.setId(100);
        task.setNom("Tâche test");
        task.setDescription("Description");
        task.setStatus("A_FAIRE");
        task.setProject(project);
    }

    // ---------------------------------------------------------------
    // GET /api/tasks/project/{projectId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /project/{projectId} - 200 : retourne la liste des tâches")
    void getByProjectId_returns200() throws Exception {
        when(taskService.getTasksByProjectId(10)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks/project/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Tâche test"))
                .andExpect(jsonPath("$[0].status").value("A_FAIRE"));

        verify(taskService).getTasksByProjectId(10);
    }

    @Test
    @DisplayName("GET /project/{projectId} - 200 : liste vide si aucune tâche")
    void getByProjectId_empty_returns200() throws Exception {
        when(taskService.getTasksByProjectId(99)).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks/project/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------
    // POST /api/tasks
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST / - 201 : tâche créée avec succès")
    void createTask_returns201() throws Exception {
        when(taskService.createTask(any(Task.class), eq(1L))).thenReturn(task);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task))
                        .header("X-Member-ID", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Tâche test"));

        verify(taskService).createTask(any(Task.class), eq(1L));
    }

    @Test
    @DisplayName("POST / - 403 : non ADMIN")
    void createTask_notAdmin_returns403() throws Exception {
        when(taskService.createTask(any(Task.class), eq(2L)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'administrateur peut créer des tâches"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task))
                        .header("X-Member-ID", 2L))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // PATCH /api/tasks/{id}/status
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PATCH /{id}/status - 200 : statut mis à jour, historique et notification déclenchés")
    void updateStatus_returns200_withHistoryAndNotification() throws Exception {
        Task taskAvecMembre = new Task();
        taskAvecMembre.setId(100);
        taskAvecMembre.setNom("Tâche test");
        taskAvecMembre.setStatus("EN_COURS");
        taskAvecMembre.setProject(task.getProject());
        taskAvecMembre.setAssignedMember(member);

        // any() car Spring désérialise "EN_COURS" (sans guillemets) depuis le JSON
        when(taskService.updateStatus(eq(100), any())).thenReturn(taskAvecMembre);
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(member));
        doNothing().when(taskHistoryService).logAction(any(), any(), any());
        doNothing().when(notificationService).sendTaskUpdateEmail(any(), any(), any(), any());

        mockMvc.perform(patch("/api/tasks/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"EN_COURS\"")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_COURS"));

        verify(taskHistoryService).logAction(eq(taskAvecMembre), eq(member), any());
        verify(notificationService).sendTaskUpdateEmail(
                eq("admin@example.com"), eq("Tâche test"), any(), eq("admin_user"));
    }

    @Test
    @DisplayName("PATCH /{id}/status - 200 : pas de notification si aucun membre assigné")
    void updateStatus_noAssignedMember_noNotification() throws Exception {
        task.setAssignedMember(null);
        // any() car Spring désérialise le JSON body sans les guillemets
        when(taskService.updateStatus(eq(100), any())).thenReturn(task);
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(member));

        mockMvc.perform(patch("/api/tasks/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"EN_COURS\"")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isOk());

        verify(notificationService, never()).sendTaskUpdateEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("PATCH /{id}/status - 200 : membre trouvé, historique logué sans notification")
    void updateStatus_memberFound_noAssignee_logsHistory() throws Exception {
        task.setAssignedMember(null);
        // any() car Spring désérialise le JSON body sans les guillemets
        when(taskService.updateStatus(eq(100), any())).thenReturn(task);
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(member));

        mockMvc.perform(patch("/api/tasks/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"EN_COURS\"")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isOk());

        verify(taskHistoryService).logAction(eq(task), eq(member), any());
        verify(notificationService, never()).sendTaskUpdateEmail(any(), any(), any(), any());
    }


    @Test
    @DisplayName("PATCH /{id}/status - 404 : tâche introuvable")
    void updateStatus_taskNotFound_returns404() throws Exception {
        when(taskService.updateStatus(eq(999), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche non trouvée"));

        mockMvc.perform(patch("/api/tasks/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"EN_COURS\"")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // PATCH /api/tasks/{id}/assign
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PATCH /{id}/assign - 200 : tâche assignée avec succès")
    void assignTask_returns200() throws Exception {
        task.setAssignedMember(member);
        AssignTaskRequest request = new AssignTaskRequest();
        request.setProjectId(10);
        request.setMemberId(2);

        when(taskService.assignTaskToMember(100, 10, 2)).thenReturn(task);

        mockMvc.perform(patch("/api/tasks/100/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(taskService).assignTaskToMember(100, 10, 2);
    }

    @Test
    @DisplayName("PATCH /{id}/assign - 400 : membre n'appartient pas au projet")
    void assignTask_memberNotInProject_returns400() throws Exception {
        AssignTaskRequest request = new AssignTaskRequest();
        request.setProjectId(10);
        request.setMemberId(99);

        when(taskService.assignTaskToMember(100, 10, 99))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce membre n'appartient pas à ce projet"));

        mockMvc.perform(patch("/api/tasks/100/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // PUT /api/tasks/{id}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PUT /{id} - 200 : tâche mise à jour, historique enregistré")
    void updateTask_returns200_withHistory() throws Exception {
        when(taskService.updateTask(eq(100), any(Task.class))).thenReturn(task);
        when(projectMemberRepository.findById(1L)).thenReturn(Optional.of(member));
        doNothing().when(taskHistoryService).logAction(any(), any(), any());

        mockMvc.perform(put("/api/tasks/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task))
                        .header("X-Member-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Tâche test"));

        verify(taskHistoryService).logAction(eq(task), eq(member), contains("100"));
    }

    @Test
    @DisplayName("PUT /{id} - 404 : tâche introuvable")
    void updateTask_notFound_returns404() throws Exception {
        when(taskService.updateTask(eq(999), any(Task.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche non trouvée"));

        mockMvc.perform(put("/api/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task))
                        .header("X-Member-ID", 1L))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // GET /api/tasks/{id}/history
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /{id}/history - 200 : retourne l'historique de la tâche")
    void getTaskHistory_returns200() throws Exception {
        TaskHistory history = new TaskHistory();
        history.setAction("CREATION");
        when(taskHistoryService.getHistoryByTaskId(100)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/tasks/100/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CREATION"));

        verify(taskHistoryService).getHistoryByTaskId(100);
    }

    @Test
    @DisplayName("GET /{id}/history - 200 : liste vide si aucun historique")
    void getTaskHistory_empty_returns200() throws Exception {
        when(taskHistoryService.getHistoryByTaskId(999)).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks/999/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------
    // DELETE /api/tasks/{id}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("DELETE /{id} - 204 : tâche supprimée avec succès")
    void deleteTask_returns204() throws Exception {
        doNothing().when(taskService).deleteTask(100, 1L);

        mockMvc.perform(delete("/api/tasks/100")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(100, 1L);
    }

    @Test
    @DisplayName("DELETE /{id} - 403 : non ADMIN")
    void deleteTask_notAdmin_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'administrateur peut supprimer une tâche"))
                .when(taskService).deleteTask(100, 2L);

        mockMvc.perform(delete("/api/tasks/100")
                        .header("X-Member-ID", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /{id} - 404 : tâche introuvable")
    void deleteTask_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "La tâche avec l'ID 999 n'existe pas"))
                .when(taskService).deleteTask(999, 1L);

        mockMvc.perform(delete("/api/tasks/999")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isNotFound());
    }
}