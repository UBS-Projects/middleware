package com.middleware.backend.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.*;
import com.middleware.backend.notification.repository.*;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationGroupRepository groupRepository;
    private final TemplateRepository templateRepository;
    private final ChannelConfigRepository channelRepository;
    //    private final NotificationLogRepository logRepository;
    private final TaskScheduler taskScheduler = createScheduler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void sendToGroup(Long groupId, Long templateId, Long channelId) {
        NotificationGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        ChannelConfig channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        // Convert JSON config into Map
        Map<String, String> config;
        try {
            config = objectMapper.readValue(channel.getConfig(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Invalid channel config JSON", e);
        }

        boolean success = true;
        String errorMsg = null;

        try {
            if (channel.getType() == ChannelType.EMAIL) {
                sendEmail(group, template, config);
            } else if (channel.getType() == ChannelType.SMS) {
                sendSms(group, template, config);
            } else {
                throw new UnsupportedOperationException("Unsupported channel type: " + channel.getType());
            }
        } catch (Exception ex) {
            success = false;
            errorMsg = ex.getMessage();
        }

        // Save log
        NotificationLog log = NotificationLog.builder()
                .group(group)
                .template(template)
                .channel(channel)
                .status(success ? "SUCCESS" : "FAILED")
                .errorMessage(errorMsg)
                .sentAt(new Timestamp(System.currentTimeMillis()))
                .build();

//        logRepository.save(log);
    }

    private void sendEmail(NotificationGroup group, NotificationTemplate template,
                           Map<String, String> config) throws Exception {

        String host = config.get("smtpHost");
        int port = Integer.parseInt(config.get("smtpPort"));
        String username = config.get("username");
        String password = config.get("password");
        String from = config.get("fromAddress");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        for (Receiver receiver : group.getReceivers()) {
            Map<String, String> receiverPlaceholders = Map.of(
                    "name", receiver.getName(),
                    "email", receiver.getEmail(),
                    "phone", receiver.getPhone()
            );

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver.getEmail()));
            message.setSubject(applyPlaceholders(template.getSubject(), receiverPlaceholders));
            message.setText(applyPlaceholders(template.getBody(), receiverPlaceholders));
            Transport.send(message);
        }
    }

    private void sendSms(NotificationGroup group, NotificationTemplate template,
                         Map<String, String> config) throws Exception {

        String ipUrl = config.get("ipUrl");
        String fromNumber = config.get("fromNumber");

        for (Receiver receiver : group.getReceivers()) {
            Map<String, String> receiverPlaceholders = Map.of(
                    "name", receiver.getName(),
                    "email", receiver.getEmail(),
                    "phone", receiver.getPhone()
            );

            String messageText = applyPlaceholders(template.getBody(), receiverPlaceholders);

            System.out.printf("Sending SMS via %s -> From: %s To: %s Body: %s%n",
                    ipUrl, fromNumber, receiver.getPhone(), messageText);

            // TODO: Replace with actual HTTP POST request to ipUrl
        }
    }

    private String applyPlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private TaskScheduler createScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void scheduleSend(Long groupId, Long templateId, Long channelId, LocalDateTime sendTime) {
        Date triggerDate = Date.from(sendTime.atZone(ZoneId.systemDefault()).toInstant());
        taskScheduler.schedule(() -> sendToGroup(groupId, templateId, channelId), triggerDate);
    }
}
