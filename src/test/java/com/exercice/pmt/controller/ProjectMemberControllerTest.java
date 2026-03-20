package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.ProjectMemberResponse;
import com.exercice.pmt.model.*;
import com.exercice.pmt.service.ProjectMemberService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectMemberController.class)
@DisplayName("ProjectMemberController - Tests")
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectMemberService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectMember projectMember;
    private ProjectMemberResponse memberResponse;
    private Role memberRole;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(2L);
        user.setUsername("jane");
        user.setEmail("jane@example.com");

        Project project = new Project();
        project.setId(10);

        memberRole = new Role();
        memberRole.setLibelle("MEMBRE");

        projectMember = new ProjectMember();
        projectMember.setId(5);
        projectMember.setUser(user);
        projectMember.setProject(project);
        projectMember.setRole(memberRole);
        projectMember.setDateArrivee(LocalDate.now());

        memberResponse = new ProjectMemberResponse(
                5, 2L, "jane", "jane@example.com", "MEMBRE", LocalDate.now()
        );
    }

    // ---------------------------------------------------------------
    // GET /api/members/project/{projectId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("GET /project/{projectId} - 200 : retourne la liste des membres")
    void getMembers_returns200WithList() throws Exception {
        when(memberService.getMembersByProject(10)).thenReturn(List.of(memberResponse));

        mockMvc.perform(get("/api/members/project/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("jane"))
                .andExpect(jsonPath("$[0].email").value("jane@example.com"))
                .andExpect(jsonPath("$[0].role").value("MEMBRE"));

        verify(memberService).getMembersByProject(10);
    }

    @Test
    @DisplayName("GET /project/{projectId} - 200 : liste vide si aucun membre")
    void getMembers_noMembers_returns200EmptyList() throws Exception {
        when(memberService.getMembersByProject(99)).thenReturn(List.of());

        mockMvc.perform(get("/api/members/project/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------
    // POST /api/members/addMember/{projectId}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("POST /addMember/{projectId} - 201 : membre ajouté avec succès")
    void inviteMember_returns201() throws Exception {
        when(memberService.addMemberByEmail(eq(10L), eq("jane@example.com"), eq("MEMBRE"), eq(1L)))
                .thenReturn(projectMember);

        Map<String, String> body = new HashMap<>();
        body.put("email", "jane@example.com");
        body.put("roleName", "MEMBRE");

        mockMvc.perform(post("/api/members/addMember/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("X-Member-ID", 1L))
                .andExpect(status().isCreated());

        verify(memberService).addMemberByEmail(10L, "jane@example.com", "MEMBRE", 1L);
    }

    @Test
    @DisplayName("POST /addMember/{projectId} - 403 : demandeur non ADMIN")
    void inviteMember_notAdmin_returns403() throws Exception {
        when(memberService.addMemberByEmail(any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'admin peut ajouter un membre"));

        Map<String, String> body = new HashMap<>();
        body.put("email", "jane@example.com");
        body.put("roleName", "MEMBRE");

        mockMvc.perform(post("/api/members/addMember/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("X-Member-ID", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /addMember/{projectId} - 409 : utilisateur déjà membre")
    void inviteMember_alreadyMember_returns409() throws Exception {
        when(memberService.addMemberByEmail(any(), any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "L'utilisateur est déjà membre du projet"));

        Map<String, String> body = new HashMap<>();
        body.put("email", "jane@example.com");
        body.put("roleName", "MEMBRE");

        mockMvc.perform(post("/api/members/addMember/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .header("X-Member-ID", 1L))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // PUT /api/members/{id}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("PUT /{id} - 200 : rôle mis à jour")
    void updateRole_returns200() throws Exception {
        when(memberService.updateMemberRole(eq(5L), any(Role.class))).thenReturn(projectMember);

        mockMvc.perform(put("/api/members/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRole)))
                .andExpect(status().isOk());

        verify(memberService).updateMemberRole(eq(5L), any(Role.class));
    }

    @Test
    @DisplayName("PUT /{id} - 404 : membre introuvable")
    void updateRole_notFound_returns404() throws Exception {
        when(memberService.updateMemberRole(eq(99L), any(Role.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));

        mockMvc.perform(put("/api/members/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRole)))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // DELETE /api/members/{id}
    // ---------------------------------------------------------------

    @Test
    @DisplayName("DELETE /{id} - 204 : membre supprimé avec succès")
    void removeMember_returns204() throws Exception {
        doNothing().when(memberService).removeMember(5L, 1L);

        mockMvc.perform(delete("/api/members/5")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isNoContent());

        verify(memberService).removeMember(5L, 1L);
    }

    @Test
    @DisplayName("DELETE /{id} - 403 : non ADMIN")
    void removeMember_notAdmin_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'admin peut ajouter un membre"))
                .when(memberService).removeMember(5L, 2L);

        mockMvc.perform(delete("/api/members/5")
                        .header("X-Member-ID", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /{id} - 404 : membre introuvable")
    void removeMember_notFound_returns404() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé"))
                .when(memberService).removeMember(99L, 1L);

        mockMvc.perform(delete("/api/members/99")
                        .header("X-Member-ID", 1L))
                .andExpect(status().isNotFound());
    }
}