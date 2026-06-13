CREATE SCHEMA IF NOT EXISTS seatsync@@

CREATE TABLE IF NOT EXISTS seatsync.seatsync_membership (
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
        INSERT INTO seatsync.seatsync_membership (
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
        INSERT INTO seatsync.seatsync_membership (
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

-- Employee View: Shows employee's seat bookings with dates and floors
CREATE OR REPLACE VIEW seatsync.employee_bookings AS
SELECT 
    b.id AS booking_id,
    u.email AS employee_email,
    u.name AS employee_name,
    u.display_name AS employee_display_name,
    u.company_id,
    u.organization_name AS workspace_name,
    b.booking_date AS booking_date,
    b.floor AS floor_number,
    b.seat_id AS seat_identifier,
    b.booking_month AS month,
    b.booking_year AS year,
    u.max_allowed_days,
    (SELECT COUNT(*) FROM seatsync.bookings b2 
     WHERE b2.company_id = u.company_id 
     AND b2.username = u.email 
     AND b2.booking_month = b.booking_month 
     AND b2.booking_year = b.booking_year) AS total_days_booked_month,
    CASE 
        WHEN (SELECT COUNT(*) FROM seatsync.bookings b2 
              WHERE b2.company_id = u.company_id 
              AND b2.username = u.email 
              AND b2.booking_month = b.booking_month 
              AND b2.booking_year = b.booking_year) >= 6 
        AND (SELECT COUNT(*) FROM seatsync.bookings b2 
              WHERE b2.company_id = u.company_id 
              AND b2.username = u.email 
              AND b2.booking_month = b.booking_month 
              AND b2.booking_year = b.booking_year) <= u.max_allowed_days 
        THEN true 
        ELSE false 
    END AS meets_criteria
FROM seatsync.seatsync_membership u
LEFT JOIN seatsync.bookings b ON u.email = b.username
WHERE u.role = 'EMPLOYEE' OR u.role IS NULL@@

-- Admin View: Shows admin's own bookings plus all employee data with criteria and extension requests
CREATE OR REPLACE VIEW seatsync.admin_dashboard AS
SELECT 
    u.email AS admin_email,
    u.name AS admin_name,
    u.display_name AS admin_display_name,
    u.company_id,
    c.company_name,
    c.company_code AS employee_join_code,
    c.admin_code AS admin_join_code,
    c.owner_name,
    c.owner_email,
    c.industry,
    c.company_size,
    -- Admin's own bookings
    (SELECT COUNT(*) FROM seatsync.bookings b 
     WHERE b.company_id = u.company_id 
     AND b.username = u.email) AS admin_total_bookings,
    -- Employee counts
    (SELECT COUNT(*) FROM seatsync.seatsync_membership eu 
     WHERE eu.company_id = u.company_id 
     AND (eu.role = 'EMPLOYEE' OR eu.role IS NULL)) AS total_employees,
    (SELECT COUNT(*) FROM seatsync.seatsync_membership au 
     WHERE au.company_id = u.company_id 
     AND au.role = 'ADMIN') AS total_admins,
    -- Pending extension requests
    (SELECT COUNT(*) FROM seatsync.booking_extension_requests ber 
     WHERE ber.company_id = u.company_id 
     AND ber.status = 'PENDING') AS pending_extension_requests
FROM seatsync.seatsync_membership u
LEFT JOIN seatsync.companies c ON u.company_id = c.id
WHERE u.role = 'ADMIN'@@

-- Admin Employee Details View: Shows each employee with their booking criteria and extension requests
CREATE OR REPLACE VIEW seatsync.admin_employee_details AS
SELECT 
    u.email AS employee_email,
    u.name AS employee_name,
    u.display_name AS employee_display_name,
    u.company_id,
    u.role,
    u.max_allowed_days,
    -- Current month booking stats
    (SELECT COUNT(*) FROM seatsync.bookings b 
     WHERE b.company_id = u.company_id 
     AND b.username = u.email 
     AND b.booking_month = EXTRACT(MONTH FROM CURRENT_DATE) 
     AND b.booking_year = EXTRACT(YEAR FROM CURRENT_DATE)) AS current_month_bookings,
    -- Criteria compliance
    CASE 
        WHEN (SELECT COUNT(*) FROM seatsync.bookings b 
              WHERE b.company_id = u.company_id 
              AND b.username = u.email 
              AND b.booking_month = EXTRACT(MONTH FROM CURRENT_DATE) 
              AND b.booking_year = EXTRACT(YEAR FROM CURRENT_DATE)) >= 6 
        THEN true 
        ELSE false 
    END AS meets_minimum_criteria,
    -- Extension requests
    ber.id AS extension_request_id,
    ber.requested_days,
    ber.extension_month,
    ber.extension_year,
    ber.status AS extension_status,
    -- Latest booking info
    (SELECT b.booking_date FROM seatsync.bookings b 
     WHERE b.company_id = u.company_id 
     AND b.username = u.email 
     ORDER BY b.booking_date DESC LIMIT 1) AS latest_booking_date,
    (SELECT b.floor FROM seatsync.bookings b 
     WHERE b.company_id = u.company_id 
     AND b.username = u.email 
     ORDER BY b.booking_date DESC LIMIT 1) AS latest_booking_floor,
    (SELECT b.seat_id FROM seatsync.bookings b 
     WHERE b.company_id = u.company_id 
     AND b.username = u.email 
     ORDER BY b.booking_date DESC LIMIT 1) AS latest_booking_seat
FROM seatsync.seatsync_membership u
LEFT JOIN seatsync.booking_extension_requests ber ON u.email = ber.username AND ber.company_id = u.company_id
WHERE u.company_id IS NOT NULL@@

-- Organization Statistics View: Shows comprehensive organization data
CREATE OR REPLACE VIEW seatsync.organization_stats AS
SELECT 
    c.id AS company_id,
    c.company_name,
    CASE WHEN c.trading_name IS NOT NULL AND c.trading_name != '' THEN c.trading_name ELSE c.company_name END AS company_display_name,
    c.company_code AS employee_join_code,
    c.admin_code AS admin_join_code,
    c.industry,
    c.company_size,
    c.owner_name,
    c.owner_email,
    c.owner_title,
    c.website,
    c.start_time,
    c.end_time,
    c.timezone,
    c.registered_at,
    -- Employee and admin counts
    (SELECT COUNT(*) FROM seatsync.seatsync_membership u 
     WHERE u.company_id = c.id 
     AND (u.role = 'EMPLOYEE' OR u.role IS NULL)) AS total_employees,
    (SELECT COUNT(*) FROM seatsync.seatsync_membership u 
     WHERE u.company_id = c.id 
     AND u.role = 'ADMIN') AS total_admins,
    -- Booking statistics
    (SELECT COUNT(*) FROM seatsync.bookings b 
     WHERE b.company_id = c.id) AS total_bookings,
    (SELECT COUNT(DISTINCT b.username) FROM seatsync.bookings b 
     WHERE b.company_id = c.id) AS active_employees_with_bookings,
    -- Current month stats
    (SELECT COUNT(*) FROM seatsync.bookings b 
     WHERE b.company_id = c.id 
     AND b.booking_month = EXTRACT(MONTH FROM CURRENT_DATE) 
     AND b.booking_year = EXTRACT(YEAR FROM CURRENT_DATE)) AS current_month_bookings,
    -- Workspace configuration
    (SELECT COUNT(*) FROM seatsync.workspace_floors wf 
     WHERE wf.company_id = c.id) AS total_floors,
    (SELECT COUNT(*) FROM seatsync.workspace_desks wd 
     WHERE wd.company_id = c.id AND wd.active = true) AS total_active_desks,
    -- Pending extension requests
    (SELECT COUNT(*) FROM seatsync.booking_extension_requests ber 
     WHERE ber.company_id = c.id 
     AND ber.status = 'PENDING') AS pending_extension_requests
FROM seatsync.companies c@@
