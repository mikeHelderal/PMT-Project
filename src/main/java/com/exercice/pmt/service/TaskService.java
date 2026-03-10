package com.exercice.pmt.service;

import com.exercice.pmt.model.Task;
import com.exercice.pmt.repository.ProjectMemberRepository;
import com.exercice.pmt.repository.TaskRepository;
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
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    public Task createTask(Task task) {
        validateMemberAccess(Long.valueOf(task.getProject().getId()), task.getAssignedMember().getId());

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
        return taskRepository.findByProjectId(projectId);
    }


    public Task updateStatus(Integer id, String newStatus){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tâche non trouvée"));
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Integer id, Task taskDetails){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Tâche non trouvée"));

        if (taskDetails.getAssignedMember() != null) {
            validateMemberAccess(Long.valueOf(task.getProject().getId()), taskDetails.getAssignedMember().getId());
            task.setAssignedMember(taskDetails.getAssignedMember());
        }

        if(taskDetails.getNom() != null){
            task.setNom(taskDetails.getNom());
        }
        if(taskDetails.getDescription() != null){
            task.setDescription(taskDetails.getDescription());
        }

        return taskRepository.save(task);
    }
}