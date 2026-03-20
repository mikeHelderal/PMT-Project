package com.exercice.pmt.service;

import com.exercice.pmt.model.*;
import com.exercice.pmt.repository.TaskHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskHistoryService - Tests unitaires")
class TaskHistoryServiceTest {

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @InjectMocks
    private TaskHistoryService taskHistoryService;

    private Task task;
    private ProjectMember author;
    private TaskHistory taskHistory;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(100);
        task.setNom("Tâche test");

        author = new ProjectMember();
        author.setId(1);

        taskHistory = new TaskHistory();
        taskHistory.setTask(task);
        taskHistory.setAuthorMember(author);
        taskHistory.setAction("CREATION");
    }

    // ---------------------------------------------------------------
    // logAction()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("logAction() - enregistre une entrée d'historique en base")
    void logAction_savesTaskHistory() {
        taskHistoryService.logAction(task, author, "CREATION");

        ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryRepository).save(captor.capture());

        TaskHistory saved = captor.getValue();
        assertThat(saved.getTask()).isEqualTo(task);
        assertThat(saved.getAuthorMember()).isEqualTo(author);
        assertThat(saved.getAction()).isEqualTo("CREATION");
    }

    @Test
    @DisplayName("logAction() - sauvegarde est appelé exactement une fois")
    void logAction_saveCalledOnce() {
        taskHistoryService.logAction(task, author, "MISE_A_JOUR");

        verify(taskHistoryRepository, times(1)).save(any(TaskHistory.class));
    }

    // ---------------------------------------------------------------
    // getHistoryByTaskId()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getHistoryByTaskId() - retourne l'historique trié par date desc")
    void getHistoryByTaskId_returnsOrderedList() {
        when(taskHistoryRepository.findByTaskIdOrderByDateActionDesc(100))
                .thenReturn(List.of(taskHistory));

        List<TaskHistory> result = taskHistoryService.getHistoryByTaskId(100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("CREATION");
        verify(taskHistoryRepository).findByTaskIdOrderByDateActionDesc(100);
    }

    @Test
    @DisplayName("getHistoryByTaskId() - retourne une liste vide si aucun historique")
    void getHistoryByTaskId_returnsEmptyList() {
        when(taskHistoryRepository.findByTaskIdOrderByDateActionDesc(999))
                .thenReturn(List.of());

        List<TaskHistory> result = taskHistoryService.getHistoryByTaskId(999);

        assertThat(result).isEmpty();
    }
}
