package com.middleware.backend.users.keycloak.service;

import com.middleware.backend.users.keycloak.dto.KeycloakTokenResponse;
import com.middleware.backend.users.keycloak.dto.KeycloakUser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@AllArgsConstructor
public class KeycloakService {

    @Value("${security.keycloak.server-url}")
    private String serverUrl;
    @Value("${security.keycloak.realm}")
    private String realm;
    @Value("${security.keycloak.client-id}")
    private String clientId;
    @Value("${security.keycloak.client-secret}")
    private String clientSecret;
    @Value("${security.keycloak.redirect-uri}")
    private String redirectUri;

    private final RestTemplate rest = new RestTemplate();

    public KeycloakTokenResponse exchangeCode(String code) {
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        ResponseEntity<KeycloakTokenResponse> res =
                rest.postForEntity(url, form, KeycloakTokenResponse.class);

        return res.getBody();
    }

    public KeycloakUser getUserInfo(String token) {
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);

        HttpEntity<Void> req = new HttpEntity<>(h);

        return rest.exchange(url, HttpMethod.GET, req, KeycloakUser.class).getBody();
    }

    public void logout(String refreshToken) {
        String url = serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        rest.postForEntity(url, form, String.class);
    }
}
