INSERT INTO public.permissions (id, name) VALUES
                                              (1, 'auditLogs:view'),
                                              (2, 'auditLogs:export'),
                                              (3, 'apiEndpoints:view'),
                                              (4, 'apiEndpoints:create'),
                                              (5, 'apiEndpoints:edit'),
                                              (6, 'apiEndpoints:delete'),
                                              (7, 'apiEndpoints:export'),
                                              (8, 'destinationApis:view'),
                                              (9, 'destinationApis:create'),
                                              (10, 'destinationApis:edit'),
                                              (11, 'destinationApis:delete'),
                                              (12, 'destinationApis:export'),
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
                                              (32, 'dynamicRoutes:export'),
                                              (33, 'middlewareLogs:view'),
                                              (34, 'middlewareLogs:export'),
                                              (35, 'jobExecutionLogs:view'),
                                              (36, 'jobExecutionLogs:viewById'),
                                              (37, 'jobExecutionLogs:export'),
                                              (38, 'scheduledJobsLogs:view'),
                                              (39, 'scheduledJobsLogs:export'),
                                              (40, 'scheduledJobs:create'),
                                              (41, 'scheduledJobs:pause'),
                                              (42, 'scheduledJobs:resume'),
                                              (43, 'scheduledJobs:edit'),
                                              (44, 'scheduledJobs:view'),
                                              (45, 'scheduledJobs:delete'),
                                              (46, 'scheduledJobs:test'),
                                              (47, 'scheduledJobs:export'),
                                              (48, 'user:view'),
                                              (49, 'user:viewById'),
                                              (50, 'user:create'),
                                              (51, 'user:delete'),
                                              (52, 'user:activate'),
                                              (53, 'user:edit'),
                                              (54, 'user:viewByRole'),
                                              (55, 'user:generate-token'),
                                              (56, 'role:view'),
                                              (57, 'role:create')
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

INSERT INTO public.users_roles (user_id, roles_id)
VALUES (1, 1)
    ON CONFLICT DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT 1, id
FROM public.permissions
WHERE id BETWEEN 1 AND 57
    ON CONFLICT DO NOTHING;


-- user
-- email: admin@mail.com
--pass: S123@231