package com.exercice.pmt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender ;

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
