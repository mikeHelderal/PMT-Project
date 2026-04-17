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

/**
 * Service orchestrant la logique métier des tâches.
 * Gère le cycle de vie complet d'une tâche : création sécurisée, assignation
 * avec contrôle d'accès, mise à jour des statuts et historisation temporelle.
 */
@Service
@RequiredArgsConstructor
public class TaskService  {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Crée une nouvelle tâche au sein d'un projet.
     * Vérifie que le demandeur possède les droits d'administration sur le projet.
     * * @param task L'entité tâche à enregistrer
     * * @param requesterMemberId ID du membre effectuant la requête
     * @return La tâche créée et persistée
     * * @throws ResponseStatusException 403 si l'utilisateur n'est pas ADMIN
     * * @throws ResponseStatusException 404 si le membre ou le projet est introuvable
     */
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

    /**
     * Valide qu'un utilisateur appartient bien à l'équipe d'un projet spécifique.
     * * @param projectId ID du projet
     * * @param userId ID de l'utilisateur à vérifier
     * @throws ResponseStatusException 400 si l'utilisateur n'est pas membre du projet
     */
    private void validateMemberAccess(Long projectId, Integer userId) {
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(Math.toIntExact(projectId), Long.valueOf(userId));
        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'utilisateur n'est pas membre de ce projet."
            );
        }
    }

    /**
     * Récupère la liste des tâches d'un projet ordonnées par identifiant.
     * * @param projectId ID du projet
     * @return Liste des tâches
     */
    public List<Task> getTasksByProjectId(Integer projectId){
        return taskRepository.findByProjectIdOrderByIdAsc(projectId);
    }


    /**
     * Met à jour uniquement le statut d'une tâche.
     * Gère automatiquement la date de fin réelle si le statut passe à "TERMINE".
     * @param id ID de la tâche
     * @param newStatus Nouveau libellé du statut
     * @return La tâche mise à jour
     */
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

    /**
     * Met à jour les détails d'une tâche de façon partielle.
     * * @param id ID de la tâche à modifier
     * * @param taskDetails Objet contenant les nouveaux champs
     * @return La tâche mise à jour
     */
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

    /**
     * Assigne une tâche à un membre spécifique après vérification de cohérence.
     * * @param taskId ID de la tâche
     * * @param projectID ID du projet (pour vérification)
     * * @param memberId ID du membre cible
     * @return La tâche mise à jour avec son nouvel attributaire
     * * @throws ResponseStatusException 400 si incohérence entre tâche, membre et projet
     */
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


    /**
     * Supprime une tâche du système.
     * Seul un administrateur du projet est autorisé à effectuer cette action.
     * * @param id ID de la tâche
     * * @param requesterMemberId ID du demandeur
     * * @throws ResponseStatusException 403 si droits insuffisants
     */
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




























