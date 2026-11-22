package com.example.notificationservice.service;

import com.example.notificationservice.dto.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi des notifications par email. Si app.mail.enabled=false (par défaut) ou si aucun serveur
 * SMTP n'est configuré, le message est simplement affiché dans la console.
 * Une erreur d'envoi ne fait jamais échouer la requête.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public NotificationService(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${app.mail.enabled:false}") boolean enabled,
                               @Value("${app.mail.from:no-reply@covoiturage.tn}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    /** @return true si un vrai email a été envoyé, false si le message a seulement été journalisé. */
    public boolean send(Notification notification) {
        JavaMailSender sender = enabled ? mailSender.getIfAvailable() : null;
        if (sender == null) {
            log.info("[EMAIL non envoyé] à={} | sujet={} | {}",
                    notification.to(), notification.subject(), notification.body());
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(notification.to());
            message.setSubject(notification.subject());
            message.setText(notification.body());
            sender.send(message);
            return true;
        } catch (RuntimeException e) {
            log.warn("Échec d'envoi de l'email à {} : {}", notification.to(), e.getMessage());
            return false;
        }
    }
}
