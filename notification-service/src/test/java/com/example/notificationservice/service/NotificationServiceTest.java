package com.example.notificationservice.service;

import com.example.notificationservice.dto.Notification;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private static final Notification N = new Notification("karim@test.tn", "Sujet", "Corps du message");

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(sender);
        return p;
    }

    @Test
    void whenDisabledTheMessageIsOnlyLogged() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationService service = new NotificationService(provider(sender), false, "no-reply@x.tn");

        assertFalse(service.send(N));
        verifyNoInteractions(sender);
    }

    @Test
    void whenNoSmtpServerIsConfiguredTheMessageIsOnlyLogged() {
        NotificationService service = new NotificationService(provider(null), true, "no-reply@x.tn");

        assertFalse(service.send(N));
    }

    @Test
    void whenEnabledTheEmailIsSent() {
        JavaMailSender sender = mock(JavaMailSender.class);
        NotificationService service = new NotificationService(provider(sender), true, "no-reply@x.tn");

        assertTrue(service.send(N));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("no-reply@x.tn", sent.getFrom());
        assertArrayEquals(new String[]{"karim@test.tn"}, sent.getTo());
        assertEquals("Sujet", sent.getSubject());
        assertEquals("Corps du message", sent.getText());
    }

    @Test
    void aSendingErrorNeverPropagates() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("SMTP down")).when(sender).send(any(SimpleMailMessage.class));
        NotificationService service = new NotificationService(provider(sender), true, "no-reply@x.tn");

        assertDoesNotThrow(() -> assertFalse(service.send(N)));
    }
}
