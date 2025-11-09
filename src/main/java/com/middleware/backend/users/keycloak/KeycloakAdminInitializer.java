package com.middleware.backend.users.keycloak;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
public class KeycloakAdminInitializer {

    @Value("${security.keycloak.server-url}")
    private String serverUrl;

    @Value("${security.keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Value("${security.keycloak.client-id:admin-cli}")
    private String clientId;

    @PostConstruct
    public void init() {
        log.info("🔐 Checking Keycloak admin user initialization...");

        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master") // use master realm for admin login
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .build()) {

            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Check if user already exists
            List<UserRepresentation> existing = usersResource.search("admin@mail.com", true);
            if (!existing.isEmpty()) {
                log.info("ℹ️ User 'admin@mail.com' already exists in Keycloak realm '{}'.", realm);
                return;
            }

            // Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername("admin@mail.com");
            user.setEmail("admin@mail.com");
            user.setEnabled(true);
            user.setEmailVerified(true);

            // Create the user
            usersResource.create(user);
            log.info("✅ Created Keycloak user: admin@mail.com");

            // Get the created user ID
            String userId = usersResource.search("admin@mail.com", true).get(0).getId();

            // Create password credentials
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue("S123@231");

            // Set password
            usersResource.get(userId).resetPassword(passwordCred);
            log.info("✅ Set password for Keycloak user: admin@mail.com");

        } catch (Exception e) {
            log.error("❌ Failed to initialize Keycloak admin user: {}", e.getMessage(), e);
        }
    }
}