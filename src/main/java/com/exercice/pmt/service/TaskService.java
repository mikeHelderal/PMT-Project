package com.exercice.pmt.service;

import com.exercice.pmt.model.Task;
import com.exercice.pmt.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public List<Task> getTasksByProjectId(Integer projectId){
        return taskRepository.findByProjectId(projectId);
    }

    public Task saveTask(Task task){
        return taskRepository.save(task);
    }

    public Task updateStatus(Integer id, String newStatus){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée"));
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    public Task updateTask(Integer id, Task taskDetails){
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Tâche non trouvée"));

        if(taskDetails.getNom() != null){
            task.setNom(taskDetails.getNom());
        }
        if(taskDetails.getDescription() != null){
            task.setDescription(taskDetails.getDescription());
        }
        return taskRepository.save(task);
    }
}
