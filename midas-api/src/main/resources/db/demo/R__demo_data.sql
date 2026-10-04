-- Demo data for the dev and test profiles only; production never loads it.
-- Every account's password is its username. Production has no seeded account:
-- its first admin comes from APP_BOOTSTRAP_ADMIN_PASSWORD (see AppLifecycleBean).
-- Repeatable migration: runs after the versioned ones and again whenever this file changes.
-- Dev and test clean the schema at start, so this file always runs on an empty schema.

-- Users
INSERT INTO users (id, public_id, username, password, rank_id, activated, banned, activation_key) VALUES
(1, '01AN4Z07BY79KA1307SR9X4MV7', 'admin', '$2a$10$RfPCTCppilPN9dR.aYkYr.nFWs2SiujkCXNRAiZtjClKkJqSMqJwW', 1, TRUE, FALSE, NULL),
(2, '01AN4Z07BY79KA1307SR9X4MV8', 'user', '$2a$10$0kzodxfrJWo27njgGaCEEO8SQZryAL1Cn6X1kQ1UbLbnUdmuQ0kkC', 2, TRUE, FALSE, NULL),
(3, '01AN4Z07BY79KA1307SR9X4MV9', 'guest', '$2a$10$wpRNBD1wYDcKraH8aAqvi.lgzDCq183EQ2/J131P5toJoCOVZvkIe', 3, TRUE, FALSE, NULL),
(4, '01AN4Z07BY79KA1307SR9X4MVA', 'moderator', '$2a$10$hIKFIOu0kaXeUx.o..D/d.FadO1ez7J4HV.mQjtxW3kU5zO.IdhV2', 4, TRUE, FALSE, NULL),
(5, '01AN4Z07BY79KA1307SR9X4MVB', 'bannedUser', '$2a$10$f3D8iQlSEvWRgZY49vV3se2yLlFxxIytj5c/kZfINGIBPV2YyXvti', 3, TRUE, TRUE, NULL),
(6, '01AN4Z07BY79KA1307SR9X4MVC', 'notActivatedUser', '$2a$10$4mLZfGWvIE1/x/AHyy2lLOqaWeL9mzpQ.vnNjxfsNUue4hkbPDIbC', 3, FALSE, FALSE, '21b8efb5-8eb0-4530-9d9a-cdbc33ba7164')
ON CONFLICT (public_id) DO NOTHING;

SELECT setval('users_id_seq', (SELECT max(id) FROM users));
