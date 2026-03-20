package com.exercice.pmt.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService - Tests unitaires")
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    // ---------------------------------------------------------------
    // sendTaskUpdateEmail()
    // ---------------------------------------------------------------

    @Test
    @DisplayName("sendTaskUpdateEmail() - envoie un email avec les bons paramètres")
    void sendTaskUpdateEmail_sendsEmailWithCorrectFields() {
        notificationService.sendTaskUpdateEmail(
                "user@example.com",
                "Tâche Importante",
                "MISE_A_JOUR",
                "john_doe"
        );

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getTo()).contains("user@example.com");
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@pmt-app.com");
        assertThat(sentMessage.getSubject()).contains("Tâche Importante");
    }

    @Test
    @DisplayName("sendTaskUpdateEmail() - mailSender.send() est appelé exactement une fois")
    void sendTaskUpdateEmail_sendCalledOnce() {
        notificationService.sendTaskUpdateEmail(
                "user@example.com", "Ma Tâche", "CREATION", "admin"
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendTaskUpdateEmail() - pas d'exception levée si mailSender échoue")
    void sendTaskUpdateEmail_mailSenderThrows_noExceptionPropagated() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() ->
                notificationService.sendTaskUpdateEmail(
                        "user@example.com", "Tâche", "ACTION", "admin"
                )
        ).doesNotThrowAnyException();
    }
}
