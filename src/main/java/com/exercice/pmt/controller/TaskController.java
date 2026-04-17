package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.AssignTaskRequest;
import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Task;
import com.exercice.pmt.model.TaskHistory;
import com.exercice.pmt.repository.ProjectMemberRepository;
import com.exercice.pmt.service.NotificationService;
import com.exercice.pmt.service.TaskHistoryService;
import com.exercice.pmt.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST supervisant le cycle de vie des tâches.
 * Gère les opérations CRUD, l'assignation des membres, le suivi de l'historique
 * et l'envoi de notifications lors des modifications de statut.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TaskController {

    private final TaskService taskService;
    private final TaskHistoryService taskHistoryService;
    private final ProjectMemberRepository projectMemberRepository;

    private final NotificationService notificationService;

    /**
     * Récupère toutes les tâches associées à un projet spécifique.
     * @param projectId Identifiant du projet
     * @return Liste des tâches du projet
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getByProjectId(@PathVariable Integer projectId) {
        return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
    }

    /**
     * Crée une nouvelle tâche et l'enregistre dans le système.
     * @param task Objet Tâche contenant les détails (nom, description, etc.)
     * @param memberId ID de l'auteur de la création (via Header X-Member-ID)
     * @return La tâche créée avec le statut 201
     */
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task, @RequestHeader("X-Member-ID") Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(task, memberId));
    }

    /**
     * Met à jour le statut d'une tâche, enregistre l'action dans l'historique
     * et notifie le membre assigné par email.
     * * @param id Identifiant de la tâche
     * * @param status Nouveau statut (ex: TODO, DONE)
     * * @param memberId ID du membre effectuant la modification
     * @return La tâche mise à jour
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(@PathVariable Integer id, @RequestBody String status,@RequestHeader("X-Member-ID") Long memberId) {
        Task updatedTask = taskService.updateStatus(id, status);

        ProjectMember currentMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        taskHistoryService.logAction(updatedTask, currentMember, "Changement de statut : " + status);
        if(updatedTask.getAssignedMember() != null && updatedTask.getAssignedMember().getUser().getEmail() != null) {
            String emailDestinataire = updatedTask.getAssignedMember().getUser().getEmail();
            String nomTache = updatedTask.getNom();
            String auteurNom = currentMember.getUser().getUsername();
            notificationService.sendTaskUpdateEmail(
                    emailDestinataire,
                    nomTache,
                    " le statut a été modifié",
                    auteurNom
            );
        }
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Assigne ou réassigne une tâche à un membre du projet.
     * * @param id Identifiant de la tâche
     * * @param request DTO contenant l'ID du projet et l'ID du membre cible
     * @return La tâche avec sa nouvelle assignation
     */
    @PatchMapping("/{id}/assign")
    public ResponseEntity<Task> assignTask(
            @PathVariable Integer id,
            @RequestBody AssignTaskRequest request
    ) {
        Task updatedTask = taskService.assignTaskToMember(
                id,
                request.getProjectId(),
                request.getMemberId()
        );

        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Met à jour les informations générales d'une tâche.
     * * @param id Identifiant de la tâche
     * * @param taskDetails Nouvelles informations de la tâche
     * * @param memberId ID du membre effectuant la mise à jour
     * @return La tâche mise à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Integer id, @RequestBody Task taskDetails,@RequestHeader("X-Member-ID") Long memberId) {
        Task updatedTask = taskService.updateTask(id, taskDetails);

        ProjectMember currentMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        taskHistoryService.logAction(updatedTask, currentMember, "Mise à jour de la tâche : " + id);

        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Récupère l'historique complet des actions effectuées sur une tâche.
     * * @param id Identifiant de la tâche
     * @return Liste chronologique des logs TaskHistory
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistory>> getTaskHistory(@PathVariable Integer id) {
        return ResponseEntity.ok(taskHistoryService.getHistoryByTaskId(id));
    }

    /**
     * Supprime définitivement une tâche.
     * * @param id Identifiant de la tâche à supprimer
     * * @param memberId ID du membre demandeur pour vérification des droits
     * @return Statut 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Task> deleteTask(@PathVariable Integer id,@RequestHeader("X-Member-ID") Long memberId) {
        taskService.deleteTask(id,memberId);
        return ResponseEntity.noContent().build();
    }
}
