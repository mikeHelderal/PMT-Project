package com.exercice.pmt.service;

import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Task;
import com.exercice.pmt.model.TaskHistory;
import com.exercice.pmt.repository.TaskHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskHistoryService {
    private final TaskHistoryRepository taskHistoryRepository;

    public void logAction(Task task, ProjectMember author, String action) {
        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setTask(task);
        taskHistory.setAuthorMember(author);
        taskHistory.setAction(action);
        taskHistoryRepository.save(taskHistory);
    }

    public List<TaskHistory> getHistoryByTaskId(int taskId) {
        return taskHistoryRepository.findByTaskIdOrderByDateActionDesc(taskId);
    }
}
