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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService  {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    public Task createTask(Task task, Long requesterMemberId) {

        ProjectMember requester = projectMemberRepository.findById(requesterMemberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Le membre n'a pas été trouvé"));

        if (!"ADMIN".equalsIgnoreCase(requester.getRole().getLibelle())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'administrateur peut créer des tâches");
        }

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

        if (task.getStatus() == null) {
            task.setStatus("A_FAIRE");
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

        if("TERMINE".equalsIgnoreCase(newStatus)) {
            task.setDateFinReelle(LocalDate.now());
        }else{
            task.setDateFinReelle(null);
        }


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

        Optional.ofNullable(taskDetails.getNom()).ifPresent(task::setNom);
        Optional.ofNullable(taskDetails.getDescription()).ifPresent(task::setDescription);
        Optional.ofNullable(taskDetails.getPriorite()).ifPresent(task::setPriorite);

        if (taskDetails.getStatus() != null) {
            String oldStatus = task.getStatus();
            String newStatus = taskDetails.getStatus();

            if (!newStatus.equals(oldStatus)) {
                task.setStatus(newStatus);
                if ("TERMINE".equalsIgnoreCase(newStatus)) {
                    task.setDateFinReelle(LocalDate.now());
                } else {
                    task.setDateFinReelle(null); // Réinitialisation si la tâche est réouverte
                }
            }
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
    @Transactional
    public void deleteTask(Integer id, Long requesterMemberId) {
        ProjectMember requester = projectMemberRepository.findById(requesterMemberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre  non trouvé"));

        if (!"ADMIN".equalsIgnoreCase(requester.getRole().getLibelle())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action refusée : Seul l'administrateur peut supprimer une tâche");
        }

        if (!taskRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La tâche avec l'ID " + id + " n'existe pas");
        }

        taskRepository.deleteById(id);
    }
}




























