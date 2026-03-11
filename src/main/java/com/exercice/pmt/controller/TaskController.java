package com.exercice.pmt.controller;

import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Task;
import com.exercice.pmt.model.TaskHistory;
import com.exercice.pmt.repository.ProjectMemberRepository;
import com.exercice.pmt.repository.TaskHistoryRepository;
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

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getByProjectId(@PathVariable Integer projectId) {
        return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {

        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(task));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(@PathVariable Integer id, @RequestBody String status,@RequestHeader("X-Member-ID") Long memberId) {
        Task updatedTask = taskService.updateStatus(id, status);
        ProjectMember currentMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        taskHistoryService.logAction(updatedTask, currentMember, "Changement de statut : " + status);
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Integer id, @RequestBody Task taskDetails,@RequestHeader("X-Member-ID") Long memberId) {
        Task updatedTask = taskService.updateTask(id, taskDetails);
        ProjectMember currentMember = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
         taskHistoryService.logAction(updatedTask, currentMember, "Mise à jour des détails de la tâche");
        return ResponseEntity.ok(updatedTask);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistory>> getTaskHistory(@PathVariable Integer id) {
        return ResponseEntity.ok(taskHistoryService.getHistoryByTaskId(id));
    }}
