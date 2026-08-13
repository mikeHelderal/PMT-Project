package com.exercice.pmt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envoi des notifications par e-mail lors des évènements survenant sur une tâche
 * (assignation, changement de statut, mise à jour).
 * <p>
 * Les envois sont asynchrones ({@link Async}) : une indisponibilité du serveur SMTP
 * ne doit jamais faire échouer l'appel REST à l'origine de la notification.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Notifie un utilisateur qu'une tâche le concernant a évolué.
     *
     * @param to        adresse e-mail du destinataire
     * @param taskName  nom de la tâche concernée
     * @param action    description de l'action réalisée (ex. « la tâche vous a été assignée »)
     * @param changedBy nom de l'utilisateur à l'origine de la modification
     */
    @Async
    public void sendTaskUpdateEmail(String to, String taskName, String action, String changedBy) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@pmt-app.com");
            message.setTo(to);
            message.setSubject("PMT - Mise à jour de la tâche : " + taskName);
            message.setText(
                    "Bonjour,\n\n"
                            + "La tâche « " + taskName + " » a été modifiée par " + changedBy + ".\n"
                            + "Action : " + action + "\n\n"
                            + "Consultez votre tableau de bord pour voir les détails.\n\n"
                            + "-- \nProject Management Tool"
            );
            mailSender.send(message);
            log.info("Notification envoyée à {} pour la tâche '{}'", to, taskName);
        } catch (Exception e) {
            // Une notification perdue ne doit pas interrompre le traitement métier
            log.error("Échec de l'envoi de la notification à {} : {}", to, e.getMessage());
        }
    }
}
