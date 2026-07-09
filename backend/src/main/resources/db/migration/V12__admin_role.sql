-- Mirrors the existing role_passenger/role_driver boolean-flag pattern, so a user can hold
-- ADMIN alongside PASSENGER/DRIVER rather than needing a separate admin account system.
ALTER TABLE users
    ADD COLUMN role_admin BOOLEAN NOT NULL DEFAULT FALSE;
