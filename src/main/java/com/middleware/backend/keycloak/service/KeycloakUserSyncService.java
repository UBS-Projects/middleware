package com.middleware.backend.keycloak.service;

import com.middleware.backend.keycloak.bootstrap.KeycloakBootstrapProps;
import com.middleware.backend.users.model.Status;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserSyncService {

    private final UserRepository userRepository;
    private final KeycloakBootstrapProps props;
    @Value("${security.keycloak.permission-client:backend-api}")
    private String permissionClientId;

    private static final boolean REMOVE_EXTRA_ROLES = true;
    private static final boolean REMOVE_EXTRA_PERMS = true;
    private static final boolean SEND_UPDATE_PASSWORD_EMAIL = true;

    @Transactional(readOnly = true)
    public void syncAllDbUsersToKeycloak(RealmResource realm) {
        UsersResource kcUsers = realm.users();

        List<User> dbUsers = userRepository.findAll();
        log.info("Syncing {} users (roles + permissions) to Keycloak…", dbUsers.size());

        Map<String, RoleRepresentation> realmRolesByName = ensureRealmRoles(realm, dbUsers);

        ClientResource permClient = resolveClient(realm, permissionClientId);
        String permClientUuid = permClient.toRepresentation().getId();
        Map<String, RoleRepresentation> clientPermsByName = preloadClientRoles(permClient);

        for (User dbUser : dbUsers) {
            String username = nvl(dbUser.getUserName());
            String email = nvl(dbUser.getEmail());
            boolean enabled = dbUser.getStatus() == null || dbUser.getStatus() == Status.ACTIVE;

            if (username.isEmpty()) {
                log.warn("Skip user with empty username (id={})", dbUser.getId());
                continue;
            }

            boolean isNew = false;
            UserRepresentation kcUser = findExactByUsername(kcUsers, username);
            if (kcUser == null) {
                UserRepresentation u = new UserRepresentation();
                u.setUsername(username);
                u.setEmail(email.isEmpty() ? null : email);
                u.setEmailVerified(!email.isEmpty());
                u.setEnabled(enabled);
                try (Response resp = kcUsers.create(u)) {
                    // ممكن تفحص resp.getStatus()
                }
                kcUser = requireExactByUsername(kcUsers, username);
                isNew = true;
                log.info("Created user '{}'", username);
            }

            UserResource ur = kcUsers.get(kcUser.getId());

            boolean changed = false;
            String kcEmail = nvl(kcUser.getEmail());
            if (!kcEmail.equals(email)) {
                kcUser.setEmail(email.isEmpty() ? null : email);
                kcUser.setEmailVerified(!email.isEmpty());
                changed = true;
            }
            if (kcUser.isEnabled() == null || kcUser.isEnabled() != enabled) {
                kcUser.setEnabled(enabled);
                changed = true;
            }
            if (changed) {
                ur.update(kcUser);
                log.info("Updated profile for '{}': email='{}', enabled={}", username, kcUser.getEmail(), enabled);
            }

            if (isNew) {
                setTemporaryPasswordAndMaybeEmail(ur, email, username);
            }

            Set<String> desiredRealmRoles = toRoleNames(dbUser.getRoles());
            List<RoleRepresentation> currentRealm = ur.roles().realmLevel().listAll();
            Set<String> currentRealmNames = currentRealm.stream()
                    .map(RoleRepresentation::getName).collect(Collectors.toSet());

            Set<String> toAddRealm = diff(desiredRealmRoles, currentRealmNames);
            if (!toAddRealm.isEmpty()) {
                List<RoleRepresentation> addReprs = toAddRealm.stream()
                        .map(realmRolesByName::get)
                        .filter(Objects::nonNull)
                        .toList();
                ur.roles().realmLevel().add(addReprs);
                log.info("Added realm roles {} to '{}'", toAddRealm, username);
            }
            if (REMOVE_EXTRA_ROLES) {
                Set<String> toRemoveRealm = diff(currentRealmNames, desiredRealmRoles);
                if (!toRemoveRealm.isEmpty()) {
                    List<RoleRepresentation> rmReprs = currentRealm.stream()
                            .filter(r -> toRemoveRealm.contains(r.getName()))
                            .toList();
                    ur.roles().realmLevel().remove(rmReprs);
                    log.info("Removed realm roles {} from '{}'", toRemoveRealm, username);
                }
            }

            Set<String> desiredPerms = new LinkedHashSet<>(
                    userRepository.findPermissionNamesByUserId(dbUser.getId()).stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
            );

            RoleScopeResource clientRoleScope = ur.roles().clientLevel(permClientUuid);
            List<RoleRepresentation> currentClient = clientRoleScope.listAll();
            Set<String> currentPerms = currentClient.stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toSet());

            Set<String> toAddPerms = diff(desiredPerms, currentPerms);
            if (!toAddPerms.isEmpty()) {
                List<RoleRepresentation> add = new ArrayList<>();
                for (String p : toAddPerms) {
                    add.add(ensureClientRole(permClient, clientPermsByName, p));
                }
                clientRoleScope.add(add);
                log.info("Added permissions {} to '{}' (client {})", toAddPerms, username, permissionClientId);
            }

            if (REMOVE_EXTRA_PERMS) {
                Set<String> toRemovePerms = diff(currentPerms, desiredPerms);
                if (!toRemovePerms.isEmpty()) {
                    List<RoleRepresentation> rm = currentClient.stream()
                            .filter(r -> toRemovePerms.contains(r.getName()))
                            .toList();
                    clientRoleScope.remove(rm);
                    log.info("Removed permissions {} from '{}' (client {})", toRemovePerms, username, permissionClientId);
                }
            }
        }

        log.info("Keycloak user/role/permission sync complete.");
    }
    public void syncUserByIdToKeycloak(Long userId) {
        try (Keycloak kc = KeycloakBuilder.builder()
                .serverUrl(props.getServerUrl())
                .realm(props.getAdminRealm())      // غالباً "master"
                .clientId("admin-cli")
                .username(props.getAdminUsername())
                .password(props.getAdminPassword())
                .build()) {

            RealmResource realm = kc.realm(props.getTargetRealm());
            // استدعي النسخة الحقيقية
            syncUserByIdToKeycloak(realm, userId);
        }
    }

    public void syncUserByIdToKeycloak(RealmResource realm, Long userId) {
        User dbUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UsersResource kcUsers = realm.users();

        Map<String, RoleRepresentation> realmRolesByName = ensureRealmRoles(realm, List.of(dbUser));
        ClientResource permClient = resolveClient(realm, permissionClientId);
        String permClientUuid = permClient.toRepresentation().getId();
        Map<String, RoleRepresentation> clientPermsByName = preloadClientRoles(permClient);

        String username = nvl(dbUser.getUserName());
        String email    = nvl(dbUser.getEmail());
        boolean enabled = dbUser.getStatus() == null || dbUser.getStatus() == Status.ACTIVE;
        if (username.isEmpty()) {
            log.warn("Skip user with empty username (id={})", dbUser.getId());
            return;
        }

        boolean isNew = false;
        UserRepresentation kcUser = findExactByUsername(kcUsers, username);
        if (kcUser == null) {
            UserRepresentation u = new UserRepresentation();
            u.setUsername(username);
            u.setEmail(email.isEmpty() ? null : email);
            u.setEmailVerified(!email.isEmpty());
            u.setEnabled(enabled);
            kcUsers.create(u);
            kcUser = requireExactByUsername(kcUsers, username);
            isNew = true;
            log.info("Created user '{}'", username);
        }
        UserResource ur = kcUsers.get(kcUser.getId());

        boolean changed = false;
        if (!Objects.equals(nvl(kcUser.getEmail()), email)) {
            kcUser.setEmail(email.isEmpty() ? null : email);
            kcUser.setEmailVerified(!email.isEmpty());
            changed = true;
        }
        if (kcUser.isEnabled() == null || kcUser.isEnabled() != enabled) {
            kcUser.setEnabled(enabled);
            changed = true;
        }
        if (changed) {
            ur.update(kcUser);
            log.info("Updated profile for '{}': email='{}', enabled={}", username, kcUser.getEmail(), enabled);
        }

        if (isNew) setTemporaryPasswordAndMaybeEmail(ur, email, username);


        Set<String> desiredRealmRoles = toRoleNames(dbUser.getRoles());
        List<RoleRepresentation> currentRealm = ur.roles().realmLevel().listAll();
        Set<String> currentRealmNames = currentRealm.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        Set<String> toAddRealm = diff(desiredRealmRoles, currentRealmNames);
        if (!toAddRealm.isEmpty()) {
            List<RoleRepresentation> addReprs = toAddRealm.stream()
                    .map(realmRolesByName::get).filter(Objects::nonNull).toList();
            ur.roles().realmLevel().add(addReprs);
            log.info("Added realm roles {} to '{}'", toAddRealm, username);
        }
        if (REMOVE_EXTRA_ROLES) {
            Set<String> toRemoveRealm = diff(currentRealmNames, desiredRealmRoles);
            if (!toRemoveRealm.isEmpty()) {
                List<RoleRepresentation> rmReprs = currentRealm.stream()
                        .filter(r -> toRemoveRealm.contains(r.getName())).toList();
                ur.roles().realmLevel().remove(rmReprs);
                log.info("Removed realm roles {} from '{}'", toRemoveRealm, username);
            }
        }

        Set<String> desiredPerms = new LinkedHashSet<>(
                userRepository.findPermissionNamesByUserId(dbUser.getId()).stream()
                        .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList()
        );

        RoleScopeResource clientRoleScope = ur.roles().clientLevel(permClientUuid);
        List<RoleRepresentation> currentClient = clientRoleScope.listAll();
        Set<String> currentPerms = currentClient.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());

        Set<String> toAddPerms = diff(desiredPerms, currentPerms);
        if (!toAddPerms.isEmpty()) {
            List<RoleRepresentation> add = new ArrayList<>();
            for (String p : toAddPerms) add.add(ensureClientRole(permClient, clientPermsByName, p));
            clientRoleScope.add(add);
            log.info("Added permissions {} to '{}' (client {})", toAddPerms, username, permissionClientId);
        }
        if (REMOVE_EXTRA_PERMS) {
            Set<String> toRemovePerms = diff(currentPerms, desiredPerms);
            if (!toRemovePerms.isEmpty()) {
                List<RoleRepresentation> rm = currentClient.stream()
                        .filter(r -> toRemovePerms.contains(r.getName())).toList();
                clientRoleScope.remove(rm);
                log.info("Removed permissions {} from '{}' (client {})", toRemovePerms, username, permissionClientId);
            }
        }
    }


    // ---------- helpers ----------

    private static Map<String, RoleRepresentation> ensureRealmRoles(RealmResource realm, List<User> users) {
        Map<String, RoleRepresentation> map = preloadRealmRoles(realm);
        Set<String> needed = users.stream()
                .filter(Objects::nonNull)
                .flatMap(u -> (u.getRoles() == null ? List.<Role>of() : u.getRoles()).stream())
                .map(Role::getRoleName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String rn : needed) {
            if (!map.containsKey(rn)) {
                RoleRepresentation rr = new RoleRepresentation();
                rr.setName(rn);
                rr.setDescription("Auto-created realm role by DB→Keycloak sync");
                realm.roles().create(rr);
                log.info("Created realm role '{}'", rn);
            }
        }
        return preloadRealmRoles(realm);
    }

    private static ClientResource resolveClient(RealmResource realm, String clientId) {
        List<ClientRepresentation> list = realm.clients().findByClientId(clientId);
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException("Required client not found: " + clientId);
        }
        return realm.clients().get(list.get(0).getId());
    }

    private static Map<String, RoleRepresentation> preloadRealmRoles(RealmResource realm) {
        Map<String, RoleRepresentation> m = new HashMap<>();
        for (RoleRepresentation r : realm.roles().list()) {
            m.put(r.getName(), r);
        }
        return m;
    }

    private static Map<String, RoleRepresentation> preloadClientRoles(ClientResource client) {
        Map<String, RoleRepresentation> m = new HashMap<>();
        for (RoleRepresentation r : client.roles().list()) {
            m.put(r.getName(), r);
        }
        return m;
    }

    private static RoleRepresentation ensureClientRole(ClientResource client,
                                                       Map<String, RoleRepresentation> cache,
                                                       String roleName) {
        RoleRepresentation rr = cache.get(roleName);
        if (rr != null) return rr;

        RoleRepresentation create = new RoleRepresentation();
        create.setName(roleName);
        create.setDescription("Auto-created permission by DB→Keycloak sync");
        client.roles().create(create);
        rr = client.roles().get(roleName).toRepresentation();
        cache.put(roleName, rr);
        log.info("Created client permission role '{}' in {}", roleName, client.toRepresentation().getClientId());
        return rr;
    }

    private static Set<String> toRoleNames(List<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(Role::getRoleName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> diff(Set<String> a, Set<String> b) {
        Set<String> copy = new LinkedHashSet<>(a);
        copy.removeAll(b);
        return copy;
    }

    private static UserRepresentation findExactByUsername(UsersResource users, String username) {
        return users.search(username, true).stream()
                .filter(u -> username.equalsIgnoreCase(u.getUsername()))
                .findFirst()
                .orElse(null);
    }

    private static UserRepresentation requireExactByUsername(UsersResource users, String username) {
        UserRepresentation u = findExactByUsername(users, username);
        if (u == null) throw new IllegalStateException("Just-created user not found: " + username);
        return u;
    }

    private static void setTemporaryPasswordAndMaybeEmail(UserResource ur, String email, String username) {
        CredentialRepresentation temp = new CredentialRepresentation();
        temp.setType(CredentialRepresentation.PASSWORD);
        temp.setTemporary(true);
        temp.setValue(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        ur.resetPassword(temp);
        log.info("Set TEMP password for '{}' (requires update on first login)", username);

        if (SEND_UPDATE_PASSWORD_EMAIL && email != null && !email.isBlank()) {
            try {
                ur.executeActionsEmail(List.of("UPDATE_PASSWORD"));
            } catch (Exception e) {
                log.warn("Could not send UPDATE_PASSWORD email to '{}': {}", username, e.getMessage());
            }
        }
    }

    private static String nvl(String s) { return s == null ? "" : s.trim(); }
}
