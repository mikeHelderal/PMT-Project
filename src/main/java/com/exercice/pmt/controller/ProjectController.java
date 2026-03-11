package com.exercice.pmt.controller;

import com.exercice.pmt.DTO.ProjectRequest;
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


    @GetMapping("/{userId}")
    public List<Project> getProjects(@PathVariable Long userId){
        return projectService.getAllProjectsByUserId(userId);
    }

    @PostMapping
    public Project create(@RequestBody ProjectRequest project){
        return projectService.saveProject(project);
    }

    @GetMapping("/project/{id}")
    public Project getProject(@PathVariable Long id){
        return projectService.getProjectById(id);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id){
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

}
