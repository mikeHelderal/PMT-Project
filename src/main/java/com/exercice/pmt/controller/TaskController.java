package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.AssignTaskRequest;
import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Task;
import com.exercice.pmt.model.TaskHistory;
import com.exercice.pmt.repository.ProjectMemberRepository;
import com.exercice.pmt.service.NotificationService;
import com.exercice.pmt.service.TaskHistoryService;
import com.exercice.pmt.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Gestion des tâches d'un projet : création, assignation, suivi et historique.
 * <p>
 * Toutes les opérations d'écriture exigent l'en-tête {@code X-Member-ID}, qui identifie
 * le membre à l'origine de la requête et permet de contrôler son rôle sur le projet.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Tâches", description = "Création, assignation, suivi et historique des tâches")
public class TaskController {

    private final TaskService taskService;
    private final TaskHistoryService taskHistoryService;
    private final ProjectMemberRepository projectMemberRepository;

    private final NotificationService notificationService;

    /**
     * Liste les tâches d'un projet, triées par identifiant croissant.
     *
     * @param projectId identifiant du projet
     * @return les tâches du projet
     */
    @Operation(
            summary = "Lister les tâches d'un projet",
            description = "Retourne toutes les tâches du projet, triées par identifiant croissant. "
                    + "Accessible à tous les rôles, y compris l'observateur : c'est la source du tableau de bord.")
    @ApiResponse(responseCode = "200", description = "Liste des tâches (éventuellement vide)")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getByProjectId(
            @Parameter(description = "Identifiant du projet", example = "1")
            @PathVariable Integer projectId) {
        return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
    }

    /**
     * Crée une tâche dans un projet.
     *
     * @param task     tâche à créer ; doit référencer un projet existant
     * @param memberId identifiant `membres_projet` du créateur (en-tête X-Member-ID)
     * @return la tâche créée
     */
    @Operation(
            summary = "Créer une tâche",
            description = """
                    Crée une tâche rattachée à un projet. Autorisé aux rôles ADMIN et MEMBER ;
                    refusé à l'observateur (GUEST). Le statut vaut `A_FAIRE` s'il n'est pas fourni.

                    Exemple de corps de requête :
                    ```json
                    {
                      "nom": "Rédiger la documentation",
                      "description": "README complet avec la procédure de déploiement",
                      "priorite": "HAUTE",
                      "dateEcheance": "2026-05-15",
                      "project": { "id": 1 }
                    }
                    ```
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tâche créée"),
            @ApiResponse(responseCode = "400", description = "Tâche non rattachée à un projet, ou assigné hors du projet"),
            @ApiResponse(responseCode = "403", description = "Un observateur ne peut pas créer de tâche"),
            @ApiResponse(responseCode = "404", description = "Membre demandeur ou projet introuvable")
    })
    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody Task task,
            @Parameter(description = "Identifiant `membres_projet` du créateur", example = "1")
            @RequestHeader("X-Member-ID") Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(task, memberId));
    }

    /**
     * Change le statut d'une tâche, journalise l'action et notifie l'assigné.
     *
     * @param id       identifiant de la tâche
     * @param status   nouveau statut (`A_FAIRE`, `EN_COURS`, `TERMINE`)
     * @param memberId identifiant `membres_projet` de l'auteur (en-tête X-Member-ID)
     * @return la tâche mise à jour
     */
    @Operation(
            summary = "Changer le statut d'une tâche",
            description = """
                    Met à jour le statut d'une tâche, enregistre l'action dans l'historique et
                    notifie par e-mail le membre assigné, s'il y en a un.

                    Le passage à `TERMINE` renseigne automatiquement la date de fin réelle ;
                    tout autre statut la remet à `null`.

                    Corps de la requête : le statut, en JSON — par exemple `"EN_COURS"`.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
            @ApiResponse(responseCode = "403", description = "Un observateur ne peut pas modifier une tâche"),
            @ApiResponse(responseCode = "404", description = "Tâche ou membre introuvable")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(
            @Parameter(description = "Identifiant de la tâche", example = "1")
            @PathVariable Integer id, @RequestBody String status,
            @Parameter(description = "Identifiant `membres_projet` de l'auteur", example = "1")
            @RequestHeader("X-Member-ID") Long memberId) {
        Task updatedTask = taskService.updateStatus(id, status, memberId);

        ProjectMember currentMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé"));

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
     * Assigne une tâche à un membre du projet, journalise l'action et le notifie.
     *
     * @param id       identifiant de la tâche
     * @param request  projet et membre destinataire de l'assignation
     * @param memberId identifiant `membres_projet` de l'auteur (en-tête X-Member-ID)
     * @return la tâche assignée
     */
    @Operation(
            summary = "Assigner une tâche à un membre",
            description = """
                    Assigne une tâche à un membre du projet. Autorisé aux rôles ADMIN et MEMBER.
                    L'action est enregistrée dans l'historique et déclenche une notification
                    e-mail auprès du membre assigné.

                    Exemple de corps de requête :
                    ```json
                    { "projectId": 1, "memberId": 2 }
                    ```
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tâche assignée"),
            @ApiResponse(responseCode = "400", description = "La tâche ou le membre n'appartient pas à ce projet"),
            @ApiResponse(responseCode = "403", description = "Un observateur ne peut pas assigner de tâche"),
            @ApiResponse(responseCode = "404", description = "Tâche ou membre introuvable")
    })
    @PatchMapping("/{id}/assign")
    public ResponseEntity<Task> assignTask(
            @Parameter(description = "Identifiant de la tâche", example = "1")
            @PathVariable Integer id,
            @RequestBody AssignTaskRequest request,
            @Parameter(description = "Identifiant `membres_projet` de l'auteur", example = "1")
            @RequestHeader("X-Member-ID") Long memberId
    ) {
        Task updatedTask = taskService.assignTaskToMember(
                id,
                request.getProjectId(),
                request.getMemberId(),
                memberId
        );

        ProjectMember currentMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé"));

        // L'assignation est une modification de la tâche : elle doit apparaître
        // dans l'historique et déclencher la notification de l'assigné.
        taskHistoryService.logAction(updatedTask, currentMember, "Assignation de la tâche à un membre");

        if (updatedTask.getAssignedMember() != null
                && updatedTask.getAssignedMember().getUser().getEmail() != null) {
            notificationService.sendTaskUpdateEmail(
                    updatedTask.getAssignedMember().getUser().getEmail(),
                    updatedTask.getNom(),
                    "la tâche vous a été assignée",
                    currentMember.getUser().getUsername()
            );
        }

        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Met à jour une tâche et journalise la modification.
     *
     * @param id          identifiant de la tâche
     * @param taskDetails champs à modifier ; seuls les champs non nuls sont appliqués
     * @param memberId    identifiant `membres_projet` de l'auteur (en-tête X-Member-ID)
     * @return la tâche mise à jour
     */
    @Operation(
            summary = "Mettre à jour une tâche",
            description = """
                    Modifie une tâche : nom, description, priorité, statut ou membre assigné.
                    Seuls les champs présents dans la requête sont appliqués. Autorisé aux
                    rôles ADMIN et MEMBER ; la modification est tracée dans l'historique.

                    Exemple de corps de requête :
                    ```json
                    { "nom": "Nouveau nom", "priorite": "MOYENNE", "status": "EN_COURS" }
                    ```
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tâche mise à jour"),
            @ApiResponse(responseCode = "400", description = "Le membre assigné n'appartient pas au projet"),
            @ApiResponse(responseCode = "403", description = "Un observateur ne peut pas modifier une tâche"),
            @ApiResponse(responseCode = "404", description = "Tâche ou membre introuvable")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @Parameter(description = "Identifiant de la tâche", example = "1")
            @PathVariable Integer id, @RequestBody Task taskDetails,
            @Parameter(description = "Identifiant `membres_projet` de l'auteur", example = "1")
            @RequestHeader("X-Member-ID") Long memberId) {
        Task updatedTask = taskService.updateTask(id, taskDetails, memberId);

        ProjectMember currentMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé"));

        taskHistoryService.logAction(updatedTask, currentMember, "Mise à jour de la tâche : " + id);

        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Retourne l'historique des modifications d'une tâche.
     *
     * @param id identifiant de la tâche
     * @return les entrées d'historique, de la plus récente à la plus ancienne
     */
    @Operation(
            summary = "Consulter l'historique d'une tâche",
            description = "Retourne les modifications successives de la tâche, de la plus récente "
                    + "à la plus ancienne, avec leur auteur. Accessible à tous les rôles.")
    @ApiResponse(responseCode = "200", description = "Historique de la tâche (éventuellement vide)")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistory>> getTaskHistory(
            @Parameter(description = "Identifiant de la tâche", example = "1")
            @PathVariable Integer id) {
        return ResponseEntity.ok(taskHistoryService.getHistoryByTaskId(id));
    }

    /**
     * Supprime une tâche. Réservé à l'administrateur du projet.
     *
     * @param id       identifiant de la tâche
     * @param memberId identifiant `membres_projet` du demandeur (en-tête X-Member-ID)
     * @return 204 si la suppression a réussi
     */
    @Operation(
            summary = "Supprimer une tâche",
            description = "Supprime définitivement une tâche. Réservé à l'administrateur du projet.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tâche supprimée"),
            @ApiResponse(responseCode = "403", description = "Seul l'administrateur peut supprimer une tâche"),
            @ApiResponse(responseCode = "404", description = "Tâche ou membre introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Task> deleteTask(
            @Parameter(description = "Identifiant de la tâche", example = "1")
            @PathVariable Integer id,
            @Parameter(description = "Identifiant `membres_projet` du demandeur (doit être ADMIN)", example = "1")
            @RequestHeader("X-Member-ID") Long memberId) {
        taskService.deleteTask(id,memberId);
        return ResponseEntity.noContent().build();
    }
}
