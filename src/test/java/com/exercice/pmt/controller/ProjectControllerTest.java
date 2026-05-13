package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.ProjectRequest;
import com.exercice.pmt.model.Project;
import com.exercice.pmt.model.User;
import com.exercice.pmt.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@DisplayName("ProjectController - Tests")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    private Project project;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin_user");

        project = new Project();
        project.setId(10);
        project.setNom("Mon Projet");
        project.setDescription("Description");
        project.setDateDebut(LocalDate.of(2024, 1, 1).atStartOfDay());
        project.setAdmin(admin);

        projectRequest = new ProjectRequest();
        projectRequest.setAdminId(1);
        projectRequest.setNom("Mon Projet");
        projectRequest.setDescription("Description");
        projectRequest.setDateDebut(LocalDate.of(2024, 1, 1).atStartOfDay());
    }

    // ---------------------------------------------------------------
    // GET /api/projects/{userId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /{userId} - 200 : retourne la liste des projets")
    void getProjects_returns200WithList() throws Exception {
        when(projectService.getAllProjectsByUserId(1L)).thenReturn(List.of(project));

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Mon Projet"))
                .andExpect(jsonPath("$[0].id").value(10));

        verify(projectService).getAllProjectsByUserId(1L);
    }

    @Test
    @DisplayName("GET /{userId} - 200 : retourne une liste vide")
    void getProjects_noProjects_returns200EmptyList() throws Exception {
        when(projectService.getAllProjectsByUserId(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------
    // POST /api/projects
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST / - 200 : projet créé avec succès")
    void createProject_returns200() throws Exception {
        when(projectService.saveProject(any(ProjectRequest.class))).thenReturn(project);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Mon Projet"));

        verify(projectService).saveProject(any(ProjectRequest.class));
    }

    @Test
    @DisplayName("POST / - admin introuvable : ServletException wrappant RuntimeException")
    void createProject_adminNotFound_throwsRuntimeException() {
        when(projectService.saveProject(any(ProjectRequest.class)))
                .thenThrow(new RuntimeException("Utilisateur introuvable"));

        jakarta.servlet.ServletException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                thrown.getCause() instanceof RuntimeException
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "Utilisateur introuvable", thrown.getCause().getMessage()
        );
    }

    // ---------------------------------------------------------------
    // GET /api/projects/project/{id}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /project/{id} - 200 : retourne le projet")
    void getProjectById_returns200() throws Exception {
        when(projectService.getProjectById(10L)).thenReturn(project);

        mockMvc.perform(get("/api/projects/project/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nom").value("Mon Projet"));
    }

    @Test
    @DisplayName("GET /project/{id} - projet introuvable : ServletException wrappant RuntimeException")
    void getProjectById_notFound_throwsRuntimeException() {
        when(projectService.getProjectById(99L))
                .thenThrow(new RuntimeException("Utilisateur introuvable"));

        jakarta.servlet.ServletException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(get("/api/projects/project/99"))
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                thrown.getCause() instanceof RuntimeException
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "Utilisateur introuvable", thrown.getCause().getMessage()
        );
    }

    // ---------------------------------------------------------------
    // DELETE /api/projects/{id}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("DELETE /{id} - 204 : projet supprimé avec succès")
    void deleteProject_returns204() throws Exception {
        doNothing().when(projectService).deleteProject(10L, 1L);

        mockMvc.perform(delete("/api/projects/10")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject(10L, 1L);
    }

    @Test
    @DisplayName("DELETE /{id} - 403 : non autorisé")
    void deleteProject_notAuthorized_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé"))
                .when(projectService).deleteProject(eq(10L), eq(99L));

        mockMvc.perform(delete("/api/projects/10")
                        .header("X-Member-ID", 99L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /{id} - 404 : projet introuvable")
    void deleteProject_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(projectService).deleteProject(eq(99L), any());

        mockMvc.perform(delete("/api/projects/99")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isNotFound());
    }
}