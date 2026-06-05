CREATE SCHEMA IF NOT EXISTS seatsync;

ALTER TABLE IF EXISTS public.booking_extension_requests SET SCHEMA seatsync;
ALTER TABLE IF EXISTS public.bookings SET SCHEMA seatsync;
ALTER TABLE IF EXISTS public.companies SET SCHEMA seatsync;
ALTER TABLE IF EXISTS public.workspace_desks SET SCHEMA seatsync;
ALTER TABLE IF EXISTS public.workspace_floors SET SCHEMA seatsync;
ALTER TABLE IF EXISTS public.users SET SCHEMA seatsync;