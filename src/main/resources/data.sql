INSERT INTO public.permissions (id, name) VALUES
                                              (1, 'auditLogs:view'),
                                              (2, 'auditLogs:export'),
                                              (3, 'apiEndpoints:view'), --to be deleted
                                              (4, 'apiEndpoints:create'), --to be deleted
                                              (5, 'apiEndpoints:edit'), --to be deleted
                                              (6, 'apiEndpoints:delete'), --to be deleted
                                              (7, 'apiEndpoints:export'), --to be deleted
                                              (8, 'destinationApis:view'), --to be deleted
                                              (9, 'destinationApis:create'), --to be deleted
                                              (10, 'destinationApis:edit'), --to be deleted
                                              (11, 'destinationApis:delete'), --to be deleted
                                              (12, 'destinationApis:export'), --to be deleted
                                              (13, 'errorCategories:view'),
                                              (14, 'errorCategories:export'),
                                              (15, 'errorCategories:create'),
                                              (16, 'errorCategories:edit'),
                                              (17, 'errorCategories:delete'),
                                              (18, 'errorMappings:view'),
                                              (19, 'errorMappings:export'),
                                              (20, 'errorMappings:create'),
                                              (21, 'errorMappings:edit'),
                                              (22, 'errorMappings:delete'),
                                              (23, 'sourceSystems:view'),
                                              (24, 'sourceSystems:export'),
                                              (25, 'sourceSystems:create'),
                                              (26, 'sourceSystems:edit'),
                                              (27, 'sourceSystems:delete'),
                                              (28, 'dynamicRoutes:view'),
                                              (29, 'dynamicRoutes:create'),
                                              (30, 'dynamicRoutes:edit'),
                                              (31, 'dynamicRoutes:delete'),
                                              (32, 'dynamicRoutes:revert'),
                                              (33, 'dynamicRoutes:stop'),
                                              (34, 'dynamicRoutes:start'),
                                              (35, 'dynamicRoutes:validate'),
                                              (36, 'dynamicRoutes:test'),
                                              (37, 'dynamicRoutes:export'),
                                              (38, 'middlewareLogs:view'),
                                              (39, 'middlewareLogs:export'),
                                              (40, 'jobExecutionLogs:view'),
                                              (41, 'jobExecutionLogs:viewById'),
                                              (42, 'jobExecutionLogs:export'),
                                              (43, 'scheduledJobsLogs:view'),
                                              (44, 'scheduledJobsLogs:export'),
                                              (45, 'scheduledJobs:create'),
                                              (46, 'scheduledJobs:pause'),
                                              (47, 'scheduledJobs:resume'),
                                              (48, 'scheduledJobs:edit'),
                                              (49, 'scheduledJobs:view'),
                                              (50, 'scheduledJobs:delete'),
                                              (51, 'scheduledJobs:test'),
                                              (52, 'scheduledJobs:export'),
                                              (53, 'user:view'),
                                              (54, 'user:viewById'),
                                              (55, 'user:create'),
                                              (56, 'user:delete'),
                                              (57, 'user:activate'),
                                              (58, 'user:edit'),
                                              (59, 'user:viewByRole'),
                                              (60, 'user:generate-token'),
                                              (61, 'role:view'),
                                              (62, 'role:create'),
                                              (63, 'dynamicRoutesLogs:view'),
                                              (64, 'integrationMapping:view'),
                                              (65, 'integrationMapping:create'),
                                              (66, 'integrationMapping:edit'),
                                              (67, 'integrationMapping:delete'),
                                              (68, 'integrationMapping:export')

    ON CONFLICT DO NOTHING;


INSERT INTO public.users
(created_at, updated_at, email, password, status, user_name)
VALUES
    (NOW(), NOW(), 'admin@mail.com',
     '$2a$10$mc8VR2mvx1FLnwLfbcWrSuJBnlRQMJbCz4R/.rVDakUs6LSuLgx3G',
     'ACTIVE', 'admin')
    ON CONFLICT (email) DO NOTHING;


-- ALTER TABLE public.role
--     ADD CONSTRAINT uq_role_name UNIQUE (role_name);
INSERT INTO public.role (role_name)
VALUES ('ADMIN')
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
WHERE id BETWEEN 1 AND 68
    ON CONFLICT(role_id, permission_id) DO NOTHING;


-- user
-- email: admin@mail.com
-- pass: S123@231