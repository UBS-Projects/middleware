package com.middleware.backend.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    //private final JavaMailSender javaMailSender;

    public void sendEmail(String to, String subject, String content) {
       /* SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
        */
        System.out.print("sending email");
    }

    public void sendWebhook(String url, String payload) {
        // Use RestTemplate to POST payload
    }

    public void sendSms(String mobile, String text) {
        // Integrate with SMS gateway provider
    }
}
