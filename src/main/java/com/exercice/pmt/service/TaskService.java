package com.exercice.pmt.service;

import com.exercice.pmt.model.Project;
import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Task;
import com.exercice.pmt.repository.ProjectMemberRepository;
import com.exercice.pmt.repository.ProjectRepository;
import com.exercice.pmt.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService  {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    public Task createTask(Task task) {
         if (task.getProject() == null || task.getProject().getId() == null) {
            throw new IllegalArgumentException("La tâche doit être rattachée à un projet existant.");
        }

        Project project = projectRepository.findById(Long.valueOf(task.getProject().getId()))
                .orElseThrow(() -> new EntityNotFoundException("Projet introuvable"));

        task.setProject(project);

        if (task.getAssignedMember() != null && task.getAssignedMember().getId() != null) {
            validateMemberAccess(
                    Long.valueOf(project.getId()),
                    task.getAssignedMember().getId()
            );
        }

        return taskRepository.save(task);
    }

    private void validateMemberAccess(Long projectId, Integer userId) {
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(Math.toIntExact(projectId), Long.valueOf(userId));
        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'utilisateur n'est pas membre de ce projet."
            );
        }
    }

    public List<Task> getTasksByProjectId(Integer projectId){
        return taskRepository.findByProjectIdOrderByIdAsc(projectId);
    }


    public Task updateStatus(Integer id, String newStatus){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche non trouvée"));
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Integer id, Task taskDetails) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche non trouvée"));

        if (taskDetails.getAssignedMember() != null && taskDetails.getAssignedMember().getId() != null) {
            validateMemberAccess(Long.valueOf(task.getProject().getId()), taskDetails.getAssignedMember().getId());
            task.setAssignedMember(taskDetails.getAssignedMember());
        }

        if (taskDetails.getNom() != null) {
            task.setNom(taskDetails.getNom());
        }
        if (taskDetails.getDescription() != null) {
            task.setDescription(taskDetails.getDescription());
        }

        if (taskDetails.getPriorite() != null) {
            task.setPriorite(taskDetails.getPriorite());
        }
        if (taskDetails.getStatus() != null) {
            task.setStatus(taskDetails.getStatus());
        }

        return taskRepository.save(task);
    }


    @Transactional
    public Task assignTaskToMember(Integer taskId, Integer projectID, Integer memberId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche non trouvé"));
        if( !task.getProject().getId().equals(projectID)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La tâche n'appartient pas à ce projet"
            );
        }
        ProjectMember member = projectMemberRepository.findById(Long.valueOf(memberId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé"));

        if(!member.getProject().getId().equals(projectID)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ce membre n'appartient pas à ce projet"
            );
        }
        task.setAssignedMember(member);
        task.setAssignee(member.getUser());
        return taskRepository.save(task);

    }

    public void deleteTask(Integer id){
        taskRepository.deleteById(id);
    }

}




























