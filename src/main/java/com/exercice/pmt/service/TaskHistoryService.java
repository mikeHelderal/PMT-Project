package com.exercice.pmt.service;

import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Task;
import com.exercice.pmt.model.TaskHistory;
import com.exercice.pmt.repository.TaskHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service dédié à l'audit et à la traçabilité des actions sur les tâches.
 * Il permet d'enregistrer chaque modification (historisation) afin de maintenir
 * un journal d'audit complet des activités au sein d'un projet.
 */
@Service
@RequiredArgsConstructor
public class TaskHistoryService {
    private final TaskHistoryRepository taskHistoryRepository;

    /**
     * Enregistre une nouvelle entrée dans l'historique d'une tâche.
     * Cette méthode capture l'état de l'action, l'auteur et la tâche concernée.
     * * @param task La tâche faisant l'objet de la modification
     * * @param author Le membre du projet ayant réalisé l'action
     * * @param action Description textuelle de la modification (ex: "Changement de statut")
     */
    public void logAction(Task task, ProjectMember author, String action) {
        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setTask(task);
        taskHistory.setAuthorMember(author);
        taskHistory.setAction(action);
        taskHistoryRepository.save(taskHistory);
    }

    /**
     * Récupère l'historique complet d'une tâche, trié par date décroissante.
     * Permet d'afficher la timeline des événements pour une tâche précise.
     * * @param taskId Identifiant de la tâche
     * @return Liste chronologique des actions enregistrées
     */
    public List<TaskHistory> getHistoryByTaskId(int taskId) {
        return taskHistoryRepository.findByTaskIdOrderByDateActionDesc(taskId);
    }
}
