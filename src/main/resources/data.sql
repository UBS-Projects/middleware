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

                                              (29, 'scheduledJobs:create'),
                                              (30, 'scheduledJobs:pause'),
                                              (31, 'scheduledJobs:resume'),
                                              (32, 'scheduledJobs:edit'),
                                              (33, 'scheduledJobs:view'),
                                              (34, 'scheduledJobs:delete'),
                                              (35, 'scheduledJobs:test'),
                                              (36, 'scheduledJobs:export'),

                                              (37, 'jobExecutionLogs:view'),
                                              (38, 'jobExecutionLogs:export'),

                                              (39, 'scheduledJobsLogs:view'),
                                              (40, 'scheduledJobsLogs:export'),

                                              (41, 'user:view'),
                                              (42, 'user:create'),
                                              (43, 'user:delete'),
                                              (44, 'user:activate'),
                                              (45, 'user:edit'),
                                              (46, 'user:viewByRole'),
                                              (47, 'user:generate-token'),

                                              (48, 'role:view'),
                                              (49, 'role:create'),

                                              (50, 'userPermissions:view'),
                                              (51, 'userPermissions:edit'),

                                              (52, 'routePermissions:view'),
                                              (53, 'routePermissions:edit'),

                                              (54, 'integrationMapping:view'),
                                              (55, 'integrationMapping:create'),
                                              (56, 'integrationMapping:edit'),
                                              (57, 'integrationMapping:delete'),
                                              (58, 'integrationMapping:export')


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
WHERE id BETWEEN 1 AND 58
    ON CONFLICT(role_id, permission_id) DO NOTHING;


-- user
-- email: admin@mail.com
-- pass: S123@231