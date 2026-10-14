CREATE ROLE user_service_migrator LOGIN PASSWORD 'user_service_migrator';
CREATE ROLE user_service_app LOGIN PASSWORD 'user_service_app';

REVOKE ALL ON DATABASE user_service FROM PUBLIC;
GRANT CONNECT ON DATABASE user_service TO user_service_migrator;
GRANT CONNECT ON DATABASE user_service TO user_service_app;

REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO user_service_migrator;
GRANT USAGE ON SCHEMA public TO user_service_app;

ALTER DEFAULT PRIVILEGES FOR ROLE user_service_migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO user_service_app;
ALTER DEFAULT PRIVILEGES FOR ROLE user_service_migrator IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO user_service_app;
