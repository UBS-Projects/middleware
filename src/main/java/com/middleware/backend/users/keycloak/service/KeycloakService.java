package com.middleware.backend.users.keycloak.service;

import com.middleware.backend.users.keycloak.dto.KeycloakTokenResponse;
import com.middleware.backend.users.keycloak.dto.KeycloakUser;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class KeycloakService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret:}")
    private String clientSecret;
    @Value("${keycloak.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public KeycloakTokenResponse exchangeCodeForToken(String code) {
        System.out.println("Exchange Code For Token Method");
        System.out.println("Code: " + code);

        String url = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("scope", "openid profile email");
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add("client_secret", clientSecret);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            return restTemplate.postForObject(url, new HttpEntity<>(params, headers), KeycloakTokenResponse.class);
        } catch (HttpClientErrorException e) {
            System.err.println("❌ KC token exchange failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.err.println("❌ KC token exchange error: " + e.getMessage());
            return null;
        }
    }

    public KeycloakUser getUserInfo(String accessToken) {
        System.out.println("getUserInfo Method");

        String url = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<KeycloakUser> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, KeycloakUser.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                System.err.println("❌ Keycloak access token expired or invalid");
                return null;
            }
            throw e;
        }
    }

    public void logoutFromKeycloak(String refreshToken) {
        try {
            String url = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            if (clientSecret != null && !clientSecret.isEmpty()) {
                params.add("client_secret", clientSecret);
            }
            if (refreshToken != null) {
                params.add("refresh_token", refreshToken);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            restTemplate.postForEntity(url, request, String.class);

            System.out.println("✅ Keycloak session invalidated successfully");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log out from Keycloak: " + e.getMessage());
        }
    }

    public String buildFrontChannelLogoutUrl(String idToken, String postLogoutRedirectUri) {
        return String.format(
                "%s/realms/%s/protocol/openid-connect/logout?id_token_hint=%s&post_logout_redirect_uri=%s",
                authServerUrl,
                realm,
                idToken,
                postLogoutRedirectUri
        );
    }
}
