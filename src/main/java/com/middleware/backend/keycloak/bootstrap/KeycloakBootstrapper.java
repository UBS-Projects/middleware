package com.middleware.backend.keycloak.bootstrap;

import com.middleware.backend.keycloak.service.KeycloakUserSyncService;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakBootstrapper {

    private final KeycloakUserSyncService keycloakUserSyncService;
    private final KeycloakBootstrapProps props;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try (Keycloak kc = KeycloakBuilder.builder()
                .serverUrl(props.getServerUrl())
                .realm(props.getAdminRealm())          //  "master"
                .clientId("admin-cli")                 // built-in
                .username(props.getAdminUsername())
                .password(props.getAdminPassword())
                .build()) {

            ensureRealm(kc, props.getTargetRealm());

            RealmResource realm = kc.realm(props.getTargetRealm());

            ensureRealmRole(realm, "admin");
            ensureRealmRole(realm, "user");

            ensureSpaClient(realm, "spa-frontend", props.getFrontendRedirects());

            ensureBearerOnlyClient(realm, "backend-api");

            keycloakUserSyncService.syncAllDbUsersToKeycloak(realm);

            // ensureAdminUser(realm, "admin", "admin@mail.com", "admin123");
        }
    }

    private void ensureRealm(Keycloak kc, String realmName) {
        RealmsResource realms = kc.realms();
        try {
            realms.realm(realmName).users().count();
            log.info("Realm '{}' already exists", realmName);
        } catch (NotFoundException nf) {
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realmName);
            rep.setEnabled(true);
            realms.create(rep);
            log.info("Realm '{}' created", realmName);
        }
    }

    private void ensureRealmRole(RealmResource realm, String roleName) {
        try {
            realm.roles().get(roleName).toRepresentation();
            log.info("Role '{}' already exists", roleName);
        } catch (NotFoundException nf) {
            RoleRepresentation rr = new RoleRepresentation();
            rr.setName(roleName);
            realm.roles().create(rr); // void
            log.info("Role '{}' created", roleName);
        }
    }

    private void ensureSpaClient(RealmResource realm, String clientId, List<String> redirects) {
        List<ClientRepresentation> found = realm.clients().findByClientId(clientId);
        if (found.isEmpty()) {
            ClientRepresentation c = new ClientRepresentation();
            c.setClientId(clientId);
            c.setName("SPA Frontend");
            c.setProtocol("openid-connect");
            c.setPublicClient(true);
            c.setStandardFlowEnabled(true);
            c.setDirectAccessGrantsEnabled(false);
            c.setImplicitFlowEnabled(false);
            c.setRedirectUris(redirects);
            c.setWebOrigins(List.of("*"));

            Map<String, String> attrs = new HashMap<>();
            attrs.put("pkce.code.challenge.method", "S256");
            attrs.put("post.logout.redirect.uris", String.join(" ", redirects));
            c.setAttributes(attrs);

            realm.clients().create(c); // void
            log.info("Client '{}' created", clientId);
        } else {
            log.info("Client '{}' already exists", clientId);
        }
    }

    private void ensureBearerOnlyClient(RealmResource realm, String clientId) {
        List<ClientRepresentation> found = realm.clients().findByClientId(clientId);
        if (found.isEmpty()) {
            ClientRepresentation c = new ClientRepresentation();
            c.setClientId(clientId);
            c.setProtocol("openid-connect");
            c.setBearerOnly(true);
            c.setStandardFlowEnabled(false);
            c.setDirectAccessGrantsEnabled(false);
            realm.clients().create(c); // void
            log.info("Bearer-only client '{}' created", clientId);
        } else {
            log.info("Bearer-only client '{}' already exists", clientId);
        }
    }


    private void ensureAdminUser(RealmResource realm, String username, String email, String password) {
        UsersResource users = realm.users();
        List<UserRepresentation> hits = users.search(username, true);
        if (hits.isEmpty()) {
            UserRepresentation u = new UserRepresentation();
            u.setUsername(username);
            u.setEmail(email);
            u.setEnabled(true);
            u.setEmailVerified(true);
            users.create(u);
            hits = users.search(username, true);
            log.info("User '{}' created", username);
        } else {
            log.info("User '{}' already exists", username);
        }

        if (!hits.isEmpty()) {
            String userId = hits.get(0).getId();

            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setTemporary(false);
            cred.setValue(password);
            users.get(userId).resetPassword(cred);

            RoleRepresentation adminRole = realm.roles().get("admin").toRepresentation();
            users.get(userId).roles().realmLevel().add(Collections.singletonList(adminRole));
            log.info("User '{}' password/roles ensured", username);
        }
    }
}
