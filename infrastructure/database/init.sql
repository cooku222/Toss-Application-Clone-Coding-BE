-- Initialize Toss database
CREATE DATABASE IF NOT EXISTS toss_db;

-- Create user
CREATE USER IF NOT EXISTS toss_user WITH PASSWORD 'toss_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE toss_db TO toss_user;

-- Connect to toss_db
\c toss_db;

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO toss_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO toss_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO toss_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO toss_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO toss_user;
