package com.exercice.pmt.controller;

import com.exercice.pmt.model.Task;
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

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getByProjectId(@PathVariable Integer projectId) {
        return ResponseEntity.ok(taskService.getTasksByProjectId(projectId));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {

        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(task));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(@PathVariable Integer id, @RequestBody String status) {
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Integer id, @RequestBody Task taskDetails) {
        Task updatedTask = taskService.updateTask(id, taskDetails);
        return ResponseEntity.ok(updatedTask);
    }
}
