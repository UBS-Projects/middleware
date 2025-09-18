//package com.middleware.backend.notification.service;
//
//import com.middleware.backend.notification.model.Notification;
//import com.middleware.backend.notification.model.Receiver;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//public class SmsSender {
//    public void send(Notification notification) {
//        // Parse channel config JSON (example: API_URL, credentials)
//        String configJson = notification.getChannel().getConfig();
//        // You can use Jackson to parse JSON if needed:
//        // ObjectMapper mapper = new ObjectMapper();
//        // SmsConfig config = mapper.readValue(configJson, SmsConfig.class);
//
//        String body = notification.getBody();
//
//        for (Receiver receiver : notification.getReceivers()) {
//            String phone = receiver.getPhone();
//
//            // Example: Using RestTemplate
//            RestTemplate restTemplate = new RestTemplate();
//            Map<String, String> request = new HashMap<>();
//            request.put("to", phone);
//            request.put("message", body);
//
//            // Replace with parsed API_URL from channel config
//            String apiUrl = "https://your-sms-api.com/send";
//
//            try {
//                restTemplate.postForEntity(apiUrl, request, String.class);
//                System.out.println("SMS sent to " + phone);
//            } catch (Exception ex) {
//                System.err.println("Failed to send SMS to " + phone + ": " + ex.getMessage());
//            }
//        }
//    }
//
//}
