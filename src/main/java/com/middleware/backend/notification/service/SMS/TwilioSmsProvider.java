package com.middleware.backend.notification.service.SMS;

import com.middleware.backend.notification.model.Receiver;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class TwilioSmsProvider implements SmsProvider {

    @Override
    public void send(Receiver receiver, String messageText, Map<String, String> config) throws Exception {
        String accountSid = config.get("accountSid");
        String authToken = config.get("authToken");
        String fromNumber = config.get("fromNumber");

        String twilioUrl = String.format(
                "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid
        );

        String auth = accountSid + ":" + authToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedAuth);

        String body = "From=" + fromNumber +
                "&To=" + receiver.getPhone() +
                "&Body=" + messageText;

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                twilioUrl, HttpMethod.POST, entity, String.class
        );

        System.out.println("Twilio Response: " + response.getBody());
    }
}

