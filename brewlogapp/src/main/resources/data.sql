MERGE INTO "user" (username, password, email, role) KEY (username)
VALUES ('admin', '$2b$10$ExSuKuNv0bXrKpbHS//P1uXlL5Yin2mzt2fM4u196ux9nG8B1nxWu', 'admin@brewlog.com', 'ADMIN');