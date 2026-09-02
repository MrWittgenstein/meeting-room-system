package com.uestcfir.service;

public interface EmailSender {
    void sendEmail(String to, String subject, String message);

    String normalizeEmail(String email);
}
