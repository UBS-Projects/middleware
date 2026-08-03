package com.middleware.backend.users.keycloak.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    @Value("${security.keycloak.server-url}")
    private String serverUrl;
    @Value("${security.keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.username}")
    private String adminUser;
    @Value("${keycloak.admin.password}")
    private String adminPass;

    @Value("${security.keycloak.client-id}")
    private String clientId;
    @Value("${security.keycloak.client-secret}")
    private String clientSecret;

    private final RestTemplate rest = new RestTemplate();

    private String getAdminAccessToken() {
        String tokenUrl = serverUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body =
                "grant_type=password" +
                        "&client_id=admin-cli" +
                        "&username=" + adminUser +
                        "&password=" + adminPass;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = rest.postForEntity(tokenUrl, request, Map.class);

        return response.getBody().get("access_token").toString();
    }

     public void createUser(String username, String email, String password) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users";

        String json = """
    {
      "username": "%s",
      "email": "%s",
      "enabled": true,
      "emailVerified": true,
      "requiredActions": [],
      "credentials": [{
        "type": "password",
        "value": "%s",
        "temporary": false
      }]
    }
    """.formatted(email, email, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<String> request = new HttpEntity<>(json, headers);
        rest.postForEntity(url, request, String.class);
    }


     public void disableUser(String email) {
        String token = getAdminAccessToken();
        String userId = getUserIdByEmail(email, token);
        if (userId == null) return;

        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;

        String json = """
        { "enabled": false }
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(json, headers), Void.class);
    }
    public void enableUser(String email) {
        String token = getAdminAccessToken();
        String userId = getUserIdByEmail(email, token);
        if (userId == null) return;

        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;

        String json = """
    { "enabled": true }
    """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(json, headers), Void.class);
    }

     public void updateEmail(String oldEmail, String newEmail) {
        String token = getAdminAccessToken();
        String userId = getUserIdByEmail(oldEmail, token);
        if (userId == null) return;

        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;

        String json = """
        {
          "email": "%s",
          "username": "%s"
        }
        """.formatted(newEmail, newEmail);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(json, headers), Void.class);
    }

     public void updatePassword(String email, String password) {
        String token = getAdminAccessToken();
        String userId = getUserIdByEmail(email, token);
        if (userId == null) return;

        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        String json = """
        {
          "type": "password",
          "value": "%s",
          "temporary": false
        }
        """.formatted(password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        rest.put(url, new HttpEntity<>(json, headers));
    }

     private String getUserIdByEmail(String email, String token) {
        String url = serverUrl + "/admin/realms/" + realm + "/users?email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map[]> response =
                rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map[].class);

        if (response.getBody().length == 0) return null;
        return response.getBody()[0].get("id").toString();
    }
    public boolean verifyUserCredentials(String email, String password) {
        try {
            String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body =
                    "grant_type=password" +
                            "&client_id=" + clientId +
                            "&client_secret=" + clientSecret +
                            "&username=" + email +
                            "&password=" + password;

            HttpEntity<String> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = rest.postForEntity(url, req, Map.class);

             if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object token = response.getBody().get("access_token");
                return token != null; // Only true if access_token exists
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
