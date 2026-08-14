//package com.middleware.backend.users.tokens.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.lang.NonNull;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//
//import jakarta.mail.internet.MimeMessage;
//
//@Service
//public class EmailService {
//
//    private final JavaMailSender sender;
//
//    @Value("${mail.from:noreply@middleware.local}")
//    private String from;
//
//    public EmailService(JavaMailSender sender) {
//        this.sender = sender;
//    }
//
//    public void sendHtml(@NonNull String to, @NonNull String subject, @NonNull String html) {
//        try {
//            MimeMessage message = sender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//            helper.setFrom(from);
//            helper.setTo(to);
//            helper.setSubject(subject);
//            helper.setText(html, true);
//            sender.send(message);
//        } catch (Exception e) {
//            // log and continue (don’t fail the whole job)
//            // use your logger
//            System.err.println("Failed sending email to " + to + ": " + e.getMessage());
//        }
//    }
//}