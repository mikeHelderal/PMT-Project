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

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TaskController {

    private final TaskService taskService;
    private final TaskHistoryService taskHistoryService;
    private final ProjectMemberRepository projectMemberRepository;

    private final NotificationService notificationService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getByProjectId(@PathVariable Integer projectId) {
        return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task, @RequestHeader("X-Member-ID") Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(task, memberId));
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Integer id, @RequestBody Task taskDetails,@RequestHeader("X-Member-ID") Long memberId) {
        Task updatedTask = taskService.updateTask(id, taskDetails);

        ProjectMember currentMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        taskHistoryService.logAction(updatedTask, currentMember, "Mise à jour de la tâche : " + id);

        return ResponseEntity.ok(updatedTask);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistory>> getTaskHistory(@PathVariable Integer id) {
        return ResponseEntity.ok(taskHistoryService.getHistoryByTaskId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Task> deleteTask(@PathVariable Integer id,@RequestHeader("X-Member-ID") Long memberId) {
        taskService.deleteTask(id,memberId);
        return ResponseEntity.noContent().build();
    }
}
