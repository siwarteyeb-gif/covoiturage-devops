package com.example.covoiturage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi d'emails (F23). Si app.mail.enabled=false (par défaut), le message est simplement
 * affiché dans la console : pratique pour la démonstration sans serveur SMTP.
 * Une erreur d'envoi ne casse jamais l'opération en cours.
 */
@Service
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.mail.from:no-reply@covoiturage.tn}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public void send(String to, String subject, String body) {
        JavaMailSender sender = enabled ? mailSender.getIfAvailable() : null;
        if (sender == null) {
            log.info("[EMAIL non envoyé] à={} | sujet={} | {}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } catch (RuntimeException e) {
            log.warn("Échec d'envoi de l'email à {} : {}", to, e.getMessage());
        }
    }
}
