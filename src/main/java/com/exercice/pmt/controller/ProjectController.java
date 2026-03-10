package com.exercice.pmt.controller;

import com.exercice.pmt.model.Project;
import com.exercice.pmt.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     *
     * @param userId
     */
    @GetMapping("user/{userId}")
    public List<Project> getProjecrs(@PathVariable Long userId){
        return projectService.getAllProjectsByUserId(userId);
    }

    @PostMapping
    public Project create(@RequestBody Project project){
        return projectService.saveProject(project);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id){
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

}
