package com.middleware.backend.notification.service.SMS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.audit_logs_interceptor.repository.NotificationLogRepository;
import com.middleware.backend.notification.dto.LogDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.*;
import com.middleware.backend.notification.repository.*;
import com.middleware.backend.notification.service.NotificationService;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationGroupRepository groupRepository;
    private final TemplateRepository templateRepository;
    private final ChannelConfigRepository channelRepository;
    private final NotificationLogRepository logRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void sendToGroup(List<String> groupCodes, String templateCode, String channelCode) {
        // Fetch template and channel
        NotificationTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        ChannelConfig channel = channelRepository.findByCode(channelCode)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        // Parse channel config JSON
        Map<String, String> config;
        try {
            config = objectMapper.readValue(channel.getConfig(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Invalid channel config JSON", e);
        }

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        for (String groupId : groupCodes) {
            NotificationGroup group = groupRepository.findByCode(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found with id " + groupId));

            try {
                // Send message
                if (channel.getType() == ChannelType.EMAIL) {
                    sendEmail(group, template, config);
                } else if (channel.getType() == ChannelType.SMS) {
                    sendSms(group, template, config);
                } else {
                    throw new UnsupportedOperationException("Unsupported channel type: " + channel.getType());
                }

                // Log success
                NotificationLog log = NotificationLog.builder()
                        .group(group)
                        .template(template)
                        .channel(channel)
                        .status("SUCCESS")
                        .userName(currentUser)
                        .sentAt(new Timestamp(System.currentTimeMillis()))
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build();
                logRepository.save(log);

            } catch (Exception ex) {
                // Log failure
                NotificationLog log = NotificationLog.builder()
                        .group(group)
                        .template(template)
                        .channel(channel)
                        .status("FAILED")
                        .errorMessage(ex.getMessage())
                        .userName(currentUser)
                        .sentAt(new Timestamp(System.currentTimeMillis()))
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build();
                logRepository.save(log);

                // Propagate exception so controller can return 500
                throw new RuntimeException(
                        "Failed to send notification for group " + groupId + ": " + ex.getMessage(), ex
                );
            }
        }
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

        for (Receiver receiver : group.getReceivers()) {
            Map<String, String> placeholders = Map.of(
                    "name", receiver.getName(),
                    "email", receiver.getEmail(),
                    "phone", receiver.getPhone()
            );
            String messageText = applyPlaceholders(template.getBody(), placeholders);

            sendSmsRequest(config, receiver.getPhone(), messageText);
        }
    }

    private void sendSmsRequest(Map<String, String> config, String to, String body) {
        String url = config.get("ipUrl");
        String method = config.getOrDefault("method", "POST").toUpperCase();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // --- Optional Auth ---
        if (config.containsKey("accountSid") && config.containsKey("authToken")) {
            String auth = Base64.getEncoder().encodeToString(
                    (config.get("accountSid") + ":" + config.get("authToken"))
                            .getBytes(StandardCharsets.UTF_8)
            );
            headers.set("Authorization", "Basic " + auth);
        }

        // --- Dynamic payload ---
        StringBuilder payload = new StringBuilder();
        if (config.containsKey("apiKey")) {
            payload.append("api_key=").append(config.get("apiKey")).append("&");
        }
        if (config.containsKey("apiSecret")) {
            payload.append("api_secret=").append(config.get("apiSecret")).append("&");
        }
        if (config.containsKey("username")) {
            payload.append("username=").append(config.get("username")).append("&");
        }
        if (config.containsKey("password")) {
            payload.append("password=").append(config.get("password")).append("&");
        }

        // Use dynamic keys
        String fromKey = config.getOrDefault("fromKey", "from");
        String toKey = config.getOrDefault("toKey", "to");
        String bodyKey = config.getOrDefault("bodyKey", "text");

        payload.append(fromKey).append("=").append(config.get("fromNumber")).append("&")
                .append(toKey).append("=").append(to).append("&")
                .append(bodyKey).append("=").append(body);

        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
        RestTemplate rest = new RestTemplate();

        try {
            ResponseEntity<String> response = rest.exchange(
                    url,
                    HttpMethod.valueOf(method),
                    entity,
                    String.class
            );
            System.out.println("SMS Response: " + response.getBody());
        } catch (Exception e) {
            System.err.println("SMS failed for " + to + ": " + e.getMessage());
        }
    }




    private String applyPlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }


    @Override
    public ResponseEntity<Page<?>> findAll(Specification<NotificationLog> spec, Pageable pageable) {
        return ResponseEntity.ok(logRepository.findAll(spec, pageable).map(r->
                LogDto.builder()
                        .id(r.getId())
                        .groupName(r.getGroup().getName())
                        .templateName(r.getTemplate().getName())
                        .channelName(r.getChannel().getName())
                        .status(r.getStatus())
                        .errorMessage(r.getErrorMessage())
                        .userName(r.getUserName())
                        .sentAt(r.getSentAt())
                        .createdAt(r.getCreatedAt())
                        .build()));
    }

    @Override
    public ResponseEntity<?> getById(Long id) {
        return ResponseEntity.ok(logRepository.findById(id).map(
                r->
                        LogDto.builder()
                                .id(r.getId())
                                .groupName(r.getGroup().getName())
                                .templateName(r.getTemplate().getName())
                                .channelName(r.getChannel().getName())
                                .status(r.getStatus())
                                .errorMessage(r.getErrorMessage())
                                .userName(r.getUserName())
                                .sentAt(r.getSentAt())
                                .createdAt(r.getCreatedAt())
                                .build()));
    }
}
