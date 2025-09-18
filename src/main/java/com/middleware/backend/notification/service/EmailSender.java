//package com.middleware.backend.notification.service;
//
//import com.middleware.backend.notification.model.Notification;
//import com.middleware.backend.notification.model.Receiver;
//import org.springframework.stereotype.Component;
//
//@Component
//public class EmailSender {
//    public void send(Notification notification) {
//        String subject = notification.getSubject();
//        String body = notification.getBody();
//
//        // loop over all receivers
//        for (Receiver receiver : notification.getReceivers()) {
//            String to = receiver.getEmail();
//
//            // Example for email
//            // mailSender.send(createMimeMessage(to, subject, body));
//
//            // Example for SMS
//            // smsClient.sendSms(receiver.getPhoneNumber(), body);
//
//            System.out.println("Sending notification to: " + to);
//        }
//    }
//}
