package com.exercice.pmt.repository;

import com.exercice.pmt.model.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory,Integer> {
    List<TaskHistory>findByTaskIdOrderByDateActionDesc(int taskId);
}
