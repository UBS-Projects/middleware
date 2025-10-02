INSERT INTO public.permissions (id, name) VALUES
                                              (1, 'auditLogs:view'),
                                              (2, 'auditLogs:export'),

                                              (3, 'errorCategories:view'),
                                              (4, 'errorCategories:export'),
                                              (5, 'errorCategories:create'),
                                              (6, 'errorCategories:edit'),

                                              (7, 'errorMappings:view'),
                                              (8, 'errorMappings:export'),
                                              (9, 'errorMappings:create'),
                                              (10, 'errorMappings:edit'),

                                              (11, 'sourceSystems:view'),
                                              (12, 'sourceSystems:export'),
                                              (13, 'sourceSystems:create'),
                                              (14, 'sourceSystems:edit'),

                                              (15, 'dynamicRoutes:view'),
                                              (16, 'dynamicRoutes:create'),
                                              (17, 'dynamicRoutes:edit'),
                                              (18, 'dynamicRoutes:delete'),
                                              (19, 'dynamicRoutes:revert'),
                                              (20, 'dynamicRoutes:stop'),
                                              (21, 'dynamicRoutes:start'),
                                              (22, 'dynamicRoutes:validate'),
                                              (23, 'dynamicRoutes:test'),
                                              (24, 'dynamicRoutes:export'),

                                              (25, 'dynamicRoutesLogs:view'),
                                              (26, 'dynamicRoutesLogs:export'),

                                              (27, 'middlewareLogs:view'),
                                              (28, 'middlewareLogs:export'),
                                              (29, 'middlewareLogs:retry'),

                                              (30, 'scheduledJobs:create'),
                                              (31, 'scheduledJobs:pause'),
                                              (32, 'scheduledJobs:resume'),
                                              (33, 'scheduledJobs:edit'),
                                              (34, 'scheduledJobs:view'),
                                              (35, 'scheduledJobs:delete'),
                                              (36, 'scheduledJobs:test'),
                                              (37, 'scheduledJobs:export'),

                                              (38, 'jobExecutionLogs:view'),
                                              (39, 'jobExecutionLogs:export'),

                                              (40, 'scheduledJobsLogs:view'),
                                              (41, 'scheduledJobsLogs:export'),

                                              (42, 'user:view'),
                                              (43, 'user:create'),
                                              (44, 'user:delete'),
                                              (45, 'user:activate'),
                                              (46, 'user:edit'),
                                              (47, 'user:viewByRole'),
                                              (48, 'user:generate-token'),

                                              (49, 'role:view'),
                                              (50, 'role:create'),

                                              (51, 'userPermissions:view'),
                                              (52, 'userPermissions:edit'),

                                              (53, 'routePermissions:view'),
                                              (54, 'routePermissions:edit'),

                                              (55, 'integrationMapping:view'),
                                              (56, 'integrationMapping:create'),
                                              (57, 'integrationMapping:edit'),
                                              (58, 'integrationMapping:delete'),
                                              (59, 'integrationMapping:export'),

                                              (60, 'dhis2:view'),
                                              (61, 'dhis2:edit'),

                                              (62, 'throttling:view'),
                                              (63, 'throttling:edit'),

                                              (64, 'notification:view'),

                                              (65, 'channel:view'),
                                              (66, 'channel:create'),
                                              (67, 'channel:edit'),
                                              (68, 'channel:delete'),

                                              (69, 'group:view'),
                                              (70, 'group:create'),
                                              (71, 'group:edit'),
                                              (72, 'group:delete'),

                                              (73, 'receiver:view'),
                                              (74, 'receiver:create'),
                                              (75, 'receiver:edit'),
                                              (76, 'receiver:delete'),

                                              (77, 'template:view'),
                                              (78, 'template:create'),
                                              (79, 'template:edit'),
                                              (80, 'template:delete'),
                                              (81, 'template:validate'),

                                              (82, 'groupReceivers:create'),

                                              (83, 'notificationLogs:view'),
                                              (84, 'notificationLogs:export')
ON CONFLICT DO NOTHING;


INSERT INTO public.users
(created_by, created_at, updated_by, updated_at, email, password, status, user_name)
VALUES
    ('SYSTEM',NOW(),'SYSTEM',NOW(), 'admin@mail.com',
     '$2a$10$mc8VR2mvx1FLnwLfbcWrSuJBnlRQMJbCz4R/.rVDakUs6LSuLgx3G',
     'ACTIVE',
     'admin')
    ON CONFLICT (email) DO NOTHING;


-- ALTER TABLE public.role
--     ADD CONSTRAINT uq_role_name UNIQUE (role_name);
INSERT INTO public.role (role_name, role_type, created_at, created_by, updated_at, updated_by)
VALUES ('ADMIN', 0, NOW(), 'SYSTEM', NOW(), 'SYSTEM')
    ON CONFLICT (role_name) DO NOTHING;



ALTER TABLE public.users_roles
DROP CONSTRAINT IF EXISTS uq_users_roles;

ALTER TABLE public.users_roles
    ADD CONSTRAINT uq_users_roles UNIQUE (user_id, roles_id);

INSERT INTO public.users_roles (user_id, roles_id)
VALUES (1, 1)
    ON CONFLICT(user_id,roles_id) DO NOTHING;


ALTER TABLE public.role_permissions
DROP CONSTRAINT IF EXISTS uq_role_permission;
ALTER TABLE public.role_permissions
    ADD CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id);

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT 1, id
FROM public.permissions
WHERE id BETWEEN 1 AND 84
    ON CONFLICT(role_id, permission_id) DO NOTHING;



-- DHIS2 instance: HMIS_DEV
INSERT INTO public.config (
    id,
    module,
    key,
    value,
    type,
    description
)
VALUES
    (gen_random_uuid(), 'dhis2', 'HMIS_DEV.code', 'HMIS_DEV', 'STRING', 'Unique DHIS2 code'),
    (gen_random_uuid(), 'dhis2', 'HMIS_DEV.baseUrl', 'hmis-dev.moh.gov.jo/dwh', 'STRING', 'DHIS2 Base URL'),
    (gen_random_uuid(), 'dhis2', 'HMIS_DEV.userName', 'supp_user', 'STRING', 'DHIS2 Username'),
    (gen_random_uuid(), 'dhis2', 'HMIS_DEV.password', 'F^*+(<2:&!^.L7:6GTtfP7>2<:6)@Z', 'STRING', 'DHIS2 Password'),
    (gen_random_uuid(), 'dhis2', 'HMIS_DEV.timeout', '30000', 'NUMBER', 'Request timeout in ms'),
    (gen_random_uuid(), 'dhis2', 'HMIS_DEV.connectTimeout', '10000', 'NUMBER', 'Connection timeout in ms')
    ON CONFLICT (key) DO NOTHING;


INSERT INTO public.rate_limit_config (
    id,
    limit_requests,
    window_seconds
)
VALUES (
           1,
           100,
           60
       )
    ON CONFLICT (id) DO NOTHING;

INSERT INTO public.camel_rate_limit (
    id,
    request_limit,
    seconds
)
VALUES (
           1,
           100,
           60
       )
    ON CONFLICT (id) DO NOTHING;


ALTER TABLE public.dynamic_route_audit
ALTER COLUMN details TYPE text;

-- user
-- INSERT INTO public.dynamic_routes
-- (route_id, description, version, path, http_method, yaml_content, active, default_version, created_at, comment)
-- VALUES
--     (
--         'dhis2-upload-csv',
--         'POST /external/integrate',
--         1,
--         '/external/integrate',
--         'POST',
--         '{ id: "dhis2-upload-csv-sync-raw-only", description: "POST /external/integrate — CSV DryRun→Commit (Beans only)", from: { uri: "servlet:/external/integrate", parameters: { httpMethodRestrict: POST } }, steps: [ { process: { ref: csvGuard } }, { process: { ref: dhis2CsvDryRunThenCommit } }, { process: { ref: dhis2ImportSummarizer } } ] }',
--         TRUE,
--         TRUE,
--         NOW(),
--         ''
--     )
--     ON CONFLICT (route_id) DO NOTHING;
--

-- email: admin@mail.com
-- pass: S123@231
