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

                                              (55, 'dhis2:view'),
                                              (56, 'dhis2:edit'),

                                              (57, 'throttling:view'),
                                              (58, 'throttling:edit'),

                                              (59, 'notification:view'),
                                              (60, 'notification:send'),

                                              (61, 'channel:view'),
                                              (62, 'channel:create'),
                                              (63, 'channel:edit'),

                                              (64, 'group:view'),
                                              (65, 'group:create'),
                                              (66, 'group:edit'),

                                              (67, 'receiver:view'),
                                              (68, 'receiver:create'),
                                              (69, 'receiver:edit'),

                                              (70, 'template:view'),
                                              (71, 'template:create'),
                                              (72, 'template:edit'),
                                              (73, 'template:validate'),

                                              (74, 'groupReceivers:create'),
                                              (75, 'groupReceivers:edit'),

                                              (76, 'notificationLogs:view'),
                                              (77, 'notificationLogs:export'),

                                              (78, 'config:view'),
                                              (79, 'config:create'),
                                              (80, 'config:edit'),

                                              (81, 'integratedSystem:view'),
                                              (82, 'integratedSystem:create'),
                                              (83, 'integratedSystem:edit'),

                                              (84, 'dashboard:summary'),
                                              (85, 'dashboard:transactionGraph'),
                                              (86, 'dashboard:jobGraph'),
                                              (87, 'dashboard:transactionList'),
                                              (88, 'dashboard:jobList'),
                                              (89, 'dashboard:tokenList'),

                                              (90, 'integratedApi:view'),
                                              (91, 'integratedApi:create'),
                                              (92, 'integratedApi:update'),
                                              (93, 'integratedApi:export'),

                                              (94, 'integrationMapping:view'),
                                              (95, 'integrationMapping:create'),
                                              (96, 'integrationMapping:edit'),
                                              (97, 'integrationMapping:validate'),
                                              (98, 'integrationMapping:export'),
                                              (99, 'integrationMapping:import')
    ON CONFLICT DO NOTHING;



INSERT INTO public.users
(created_by, created_at, updated_by, updated_at, email, status, user_name)
VALUES
    ('SYSTEM',NOW(),'SYSTEM',NOW(), 'admin@mail.com',

     'ACTIVE',
     'admin')
    ON CONFLICT (email) DO NOTHING;

-- '$2a$10$xBgaTDblE6qOZAAuJsMVWeWfuqifp8TyNGmxKeKp6JB3zDxh6gdIe',
-- ALTER TABLE public.role
--     ADD CONSTRAINT uq_role_name UNIQUE (role_name);
INSERT INTO public.role (role_name, role_type, created_at, created_by, updated_at, updated_by)
VALUES ('ADMIN', 0, NOW(), 'SYSTEM', NOW(), 'SYSTEM')
    ON CONFLICT (role_name) DO NOTHING;

INSERT INTO public.role (role_name, role_type, created_at, created_by, updated_at, updated_by)
VALUES ('EMAIL_SENDER', 1, NOW(), 'SYSTEM', NOW(), 'SYSTEM')
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
WHERE id BETWEEN 1 AND 99
    ON CONFLICT(role_id, permission_id) DO NOTHING;



-- DHIS2 instance: HMIS_DEV
INSERT INTO public.config (
    id,
    key,
    value,
    type,
    description,
    created_by,
    created_at,
    updated_by,
    updated_at
)
VALUES
    (gen_random_uuid(), 'API_TIME_OUT', '3000', 'NUMBER', 'TIMEOUT FOR AN API', 'System', NOW(), 'System', NOW())
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
           200,
           60
       )
    ON CONFLICT (id) DO NOTHING;


ALTER TABLE public.dynamic_route_audit
ALTER COLUMN details TYPE text;


INSERT INTO public.integrated_system (
    id,
    code,
    host,
    port,
    description,
    protocol,
    additional_key1,
    additional_value1,
    additional_key2,
    additional_value2,
    authentication_type,
    username,
    password,
    token,
    created_by,
    created_at,
    updated_by,
    updated_at
)VALUES
         (1,'DHIS2.1','play.im.dhis2.org/stable-2-41-5',443,'DHIS2 Instance one','HTTPS','timeout',30000,'connect-timeout',10000,'BASIC','admin','district','','SYSTEM',NOW(),'SYSTEM',NOW()),
         (2,'DHIS2.2','hmis-dev.moh.gov.jo/dwh',443,'DHIS2 Instance two','HTTPS','timeout',30000,'connect-timeout',10000,'BASIC','supp_user','F^*+(<2:&!^.L7:6GTtfP7>2<:6)@Z','','SYSTEM',NOW(),'SYSTEM',NOW())
        ON CONFLICT (code) DO NOTHING;



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
