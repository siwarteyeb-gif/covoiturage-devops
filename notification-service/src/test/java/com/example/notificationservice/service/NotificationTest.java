package com.example.notificationservice.service;

import com.example.notificationservice.dto.Notification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void aCompleteNotificationIsValid() {
        assertTrue(new Notification("a@b.tn", "Sujet", "Corps").isValid());
    }

    @Test
    void missingOrMalformedFieldsAreInvalid() {
        assertFalse(new Notification(null, "Sujet", "Corps").isValid());
        assertFalse(new Notification("sans-arobase", "Sujet", "Corps").isValid());
        assertFalse(new Notification("a@b.tn", " ", "Corps").isValid());
        assertFalse(new Notification("a@b.tn", "Sujet", null).isValid());
    }
}
