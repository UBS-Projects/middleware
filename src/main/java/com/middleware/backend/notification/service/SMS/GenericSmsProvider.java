package com.middleware.backend.notification.service.SMS;
import com.middleware.backend.notification.model.Receiver;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
public class GenericSmsProvider implements SmsProvider{
    @Override
    public void send(Receiver receiver, String messageText, Map<String, String> config) throws Exception {
        String ipUrl = config.get("ipUrl");
        String apiKey = config.get("apiKey");
        String apiSecret = config.get("apiSecret");
        String fromNumber = config.get("fromNumber");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "from=" + fromNumber +
                "&to=" + receiver.getPhone() +
                "&text=" + messageText +
                "&api_key=" + apiKey +
                "&api_secret=" + apiSecret;

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                ipUrl, HttpMethod.POST, entity, String.class
        );

        System.out.println("Generic Provider Response: " + response.getBody());
    }
}
