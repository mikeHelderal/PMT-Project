package com.exercice.pmt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service de communication chargé de l'envoi des notifications.
 * Ce service utilise JavaMailSender pour expédier des alertes par courriel
 * de manière asynchrone afin de ne pas bloquer le thread principal de l'application.
 */
@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender ;

    /**
     * Envoie un email de notification suite à la mise à jour d'une tâche.
     * Cette méthode est exécutée de façon asynchrone.
     * * @param to Adresse email du destinataire (généralement le membre assigné)
     * * @param taskName Nom de la tâche concernée
     * * @param action Description de la modification effectuée
     * * @param changedBy Nom de l'utilisateur ayant réalisé l'action
     */
    @Async
    public void sendTaskUpdateEmail(String to, String taskName, String action, String changedBy) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@pmt-app.com");
            message.setTo(to);
            message.setSubject("PMT - Mise à jour de la tâche : " + taskName);
            message.setText("Bonjour, \n\n"+
                    "La tâche'\" + taskName + \"' a été modifiée par \" + changedBy + \".\\n\" +\n" +
                    "                    \"Action : \" + action + \"\\n\\n\" +\n" +
                    "                    \"Consultez votre dashboard pour voir les détails.");
            mailSender.send(message);
            System.out.println("email envoyé avec succès à : " + to);
        } catch (Exception e) {
            System.out.println("Erreur lors del 'envoie de l'email : " + e.getMessage());
        }
    }
}
