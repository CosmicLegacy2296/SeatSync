CREATE SCHEMA IF NOT EXISTS "SeatSync"@@

CREATE TABLE IF NOT EXISTS "SeatSync".seatsync_membership (
    email VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255),
    name VARCHAR(255),
    display_name VARCHAR(255),
    max_allowed_days INTEGER,
    company_id VARCHAR(255),
    organization_name VARCHAR(255),
    role VARCHAR(255)
)@@

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'users'
    ) THEN
        INSERT INTO "SeatSync".seatsync_membership (
            email, password, name, display_name,
            max_allowed_days, company_id, organization_name, role
        )
        SELECT
            email, password, name, display_name,
            max_allowed_days, company_id, organization_name, role
        FROM public.users
        ON CONFLICT (email) DO UPDATE SET
            password = EXCLUDED.password,
            name = EXCLUDED.name,
            display_name = EXCLUDED.display_name,
            max_allowed_days = EXCLUDED.max_allowed_days,
            company_id = EXCLUDED.company_id,
            organization_name = EXCLUDED.organization_name,
            role = EXCLUDED.role;

        DROP TABLE public.users;
    END IF;
END $$@@

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'seatsync' AND table_name = 'users'
    ) THEN
        INSERT INTO "SeatSync".seatsync_membership (
            email, password, name, display_name,
            max_allowed_days, company_id, organization_name, role
        )
        SELECT
            email, password, name, display_name,
            max_allowed_days, company_id, organization_name, role
        FROM seatsync.users
        ON CONFLICT (email) DO UPDATE SET
            password = EXCLUDED.password,
            name = EXCLUDED.name,
            display_name = EXCLUDED.display_name,
            max_allowed_days = EXCLUDED.max_allowed_days,
            company_id = EXCLUDED.company_id,
            organization_name = EXCLUDED.organization_name,
            role = EXCLUDED.role;

        DROP TABLE seatsync.users;
    END IF;
END $$@@
