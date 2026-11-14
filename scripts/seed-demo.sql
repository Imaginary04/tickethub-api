-- TicketHub demo data for local development (NOT a Flyway migration: never runs automatically).
--
-- Adds 4 venues, 8 published events + 1 draft, and 15 shows (Nov 2026 - Jan 2027).
-- Everything belongs to the organizer account organizer@test.com, which must already exist.
-- Runs in one transaction and refuses to run twice.
--
-- Run from the tickethub-api folder (PowerShell), with Docker running:
--   Get-Content scripts\seed-demo.sql -Raw | docker exec -i tickethub-postgres psql -U tickethub -d tickethub

\set ON_ERROR_STOP on
BEGIN;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'organizer@test.com') THEN
    RAISE EXCEPTION 'Create the account organizer@test.com (role ORGANIZER) first';
  END IF;
  IF EXISTS (SELECT 1 FROM events WHERE title = 'Neon Nights Live') THEN
    RAISE EXCEPTION 'Demo data already loaded';
  END IF;
END $$;

-- Venues
INSERT INTO venues (organizer_id, name, city, address)
SELECT u.id, v.name, v.city, v.address
FROM users u,
     (VALUES ('Skyline Arena',       'Mumbai',    'Lower Parel'),
             ('Riverside Hall',      'Pune',      'Koregaon Park'),
             ('Garden City Grounds', 'Bengaluru', 'Palace Road'),
             ('Capital Theatre',     'Delhi',     'Connaught Place')) AS v(name, city, address)
WHERE u.email = 'organizer@test.com';

-- PVR Screen 3 (Mumbai) is used by "The Last Monsoon"; create it if this database doesn't have it yet
INSERT INTO venues (organizer_id, name, city, address)
SELECT u.id, 'PVR Screen 3', 'Mumbai', 'Andheri'
FROM users u
WHERE u.email = 'organizer@test.com'
  AND NOT EXISTS (SELECT 1 FROM venues WHERE name = 'PVR Screen 3');

-- Seat layouts: one row per (venue, row label, seats, category)
INSERT INTO seats (venue_id, row_label, seat_number, category)
SELECT v.id, r.label, n, r.category
FROM (VALUES ('Skyline Arena', 'A', 20, 'PLATINUM'), ('Skyline Arena', 'B', 20, 'PLATINUM'),
             ('Skyline Arena', 'C', 30, 'GOLD'),     ('Skyline Arena', 'D', 30, 'GOLD'),
             ('Skyline Arena', 'E', 30, 'GOLD'),     ('Skyline Arena', 'F', 40, 'SILVER'),
             ('Skyline Arena', 'G', 40, 'SILVER'),   ('Skyline Arena', 'H', 40, 'SILVER'),
             ('Riverside Hall', 'A', 25, 'GOLD'),    ('Riverside Hall', 'B', 25, 'GOLD'),
             ('Riverside Hall', 'C', 30, 'SILVER'),  ('Riverside Hall', 'D', 30, 'SILVER'),
             ('Riverside Hall', 'E', 30, 'SILVER'),
             ('Garden City Grounds', 'A', 50, 'PLATINUM'), ('Garden City Grounds', 'B', 60, 'GOLD'),
             ('Garden City Grounds', 'C', 60, 'GOLD'),     ('Garden City Grounds', 'D', 80, 'SILVER'),
             ('Garden City Grounds', 'E', 80, 'SILVER'),  ('Garden City Grounds', 'F', 80, 'SILVER'),
             ('Capital Theatre', 'A', 15, 'PLATINUM'), ('Capital Theatre', 'B', 20, 'GOLD'),
             ('Capital Theatre', 'C', 20, 'GOLD'),     ('Capital Theatre', 'D', 25, 'SILVER'),
             ('Capital Theatre', 'E', 25, 'SILVER'),
             ('PVR Screen 3', 'A', 10, 'PLATINUM'), ('PVR Screen 3', 'B', 12, 'GOLD'),
             ('PVR Screen 3', 'C', 15, 'SILVER')) AS r(venue, label, seats, category)
JOIN venues v ON v.name = r.venue
CROSS JOIN LATERAL generate_series(1, r.seats) AS n
-- Skip venues that already have seats (e.g. an existing PVR Screen 3)
WHERE NOT EXISTS (SELECT 1 FROM seats s WHERE s.venue_id = v.id);

-- Events (8 published + 1 draft)
INSERT INTO events (organizer_id, title, description, category, status)
SELECT u.id, e.title, e.description, e.category, e.status
FROM users u,
     (VALUES ('Neon Nights Live', 'An electronic music night with a full light show.', 'CONCERT', 'PUBLISHED'),
             ('Laugh Riot: Stand-up Special', 'Three comedians, one evening, no filter.', 'COMEDY', 'PUBLISHED'),
             ('The Last Monsoon', 'Premiere screening of a coastal drama, followed by a Q&A.', 'MOVIE', 'PUBLISHED'),
             ('Hamlet Reimagined', 'Shakespeare''s classic, set in present-day Delhi.', 'THEATRE', 'PUBLISHED'),
             ('Kabaddi Night: City Derby', 'Floodlit derby between the two top local clubs.', 'SPORTS', 'PUBLISHED'),
             ('Photography Basics Workshop', 'Hands-on morning workshop: light, framing and editing.', 'WORKSHOP', 'PUBLISHED'),
             ('Sufi Evening', 'Qawwali and Sufi poetry under the stars.', 'CONCERT', 'PUBLISHED'),
             ('New Year''s Eve Bash', 'Count down to 2027 with live DJs.', 'OTHER', 'PUBLISHED'),
             ('Secret Acoustic Set', 'Draft: not visible to the public.', 'CONCERT', 'DRAFT')) AS e(title, description, category, status)
WHERE u.email = 'organizer@test.com';

-- Shows (times in India time)
INSERT INTO shows (event_id, venue_id, start_time, end_time)
SELECT e.id, v.id, s.start_time::timestamptz, s.end_time::timestamptz
FROM (VALUES ('The Last Monsoon',            'PVR Screen 3',        '2026-11-20 18:00+05:30', '2026-11-20 20:30+05:30'),
             ('The Last Monsoon',            'PVR Screen 3',        '2026-11-20 21:30+05:30', '2026-11-21 00:00+05:30'),
             ('Laugh Riot: Stand-up Special', 'Capital Theatre',    '2026-11-22 20:00+05:30', '2026-11-22 22:00+05:30'),
             ('Photography Basics Workshop', 'Riverside Hall',      '2026-11-28 10:00+05:30', '2026-11-28 13:00+05:30'),
             ('Laugh Riot: Stand-up Special', 'Skyline Arena',      '2026-11-29 20:00+05:30', '2026-11-29 22:00+05:30'),
             ('Neon Nights Live',            'Skyline Arena',       '2026-12-05 19:30+05:30', '2026-12-05 23:00+05:30'),
             ('Neon Nights Live',            'Riverside Hall',      '2026-12-07 19:00+05:30', '2026-12-07 22:30+05:30'),
             ('Hamlet Reimagined',           'Capital Theatre',     '2026-12-12 19:00+05:30', '2026-12-12 21:45+05:30'),
             ('Hamlet Reimagined',           'Capital Theatre',     '2026-12-13 19:00+05:30', '2026-12-13 21:45+05:30'),
             ('Kabaddi Night: City Derby',   'Garden City Grounds', '2026-12-20 18:00+05:30', '2026-12-20 21:00+05:30'),
             ('New Year''s Eve Bash',        'Skyline Arena',       '2026-12-31 21:00+05:30', '2027-01-01 01:00+05:30'),
             ('Sufi Evening',                'Garden City Grounds', '2027-01-10 19:00+05:30', '2027-01-10 22:00+05:30'),
             ('Sufi Evening',                'Riverside Hall',      '2027-01-16 19:00+05:30', '2027-01-16 22:00+05:30'),
             ('Secret Acoustic Set',         'Skyline Arena',       '2027-01-20 19:00+05:30', '2027-01-20 21:00+05:30'),
             ('Photography Basics Workshop', 'Riverside Hall',      '2027-01-24 10:00+05:30', '2027-01-24 13:00+05:30')
     ) AS s(event, venue, start_time, end_time)
JOIN events e ON e.title = s.event
JOIN venues v ON v.name = s.venue;

-- Prices: each event's price list, applied only to the categories its show's venue actually has
INSERT INTO show_pricing (show_id, seat_category, price)
SELECT sh.id, p.category, p.price
FROM shows sh
JOIN events e ON e.id = sh.event_id
JOIN (VALUES ('Neon Nights Live', 'PLATINUM', 4999.00), ('Neon Nights Live', 'GOLD', 2499.00), ('Neon Nights Live', 'SILVER', 1199.00),
             ('Laugh Riot: Stand-up Special', 'PLATINUM', 1499.00), ('Laugh Riot: Stand-up Special', 'GOLD', 999.00), ('Laugh Riot: Stand-up Special', 'SILVER', 599.00),
             ('The Last Monsoon', 'PLATINUM', 450.00), ('The Last Monsoon', 'GOLD', 320.00), ('The Last Monsoon', 'SILVER', 220.00),
             ('Hamlet Reimagined', 'PLATINUM', 1800.00), ('Hamlet Reimagined', 'GOLD', 1200.00), ('Hamlet Reimagined', 'SILVER', 700.00),
             ('Kabaddi Night: City Derby', 'PLATINUM', 1500.00), ('Kabaddi Night: City Derby', 'GOLD', 800.00), ('Kabaddi Night: City Derby', 'SILVER', 350.00),
             ('Photography Basics Workshop', 'PLATINUM', 2500.00), ('Photography Basics Workshop', 'GOLD', 1999.00), ('Photography Basics Workshop', 'SILVER', 1499.00),
             ('Sufi Evening', 'PLATINUM', 2200.00), ('Sufi Evening', 'GOLD', 1400.00), ('Sufi Evening', 'SILVER', 650.00),
             ('New Year''s Eve Bash', 'PLATINUM', 6000.00), ('New Year''s Eve Bash', 'GOLD', 3500.00), ('New Year''s Eve Bash', 'SILVER', 1800.00),
             ('Secret Acoustic Set', 'PLATINUM', 999.00), ('Secret Acoustic Set', 'GOLD', 799.00), ('Secret Acoustic Set', 'SILVER', 499.00)
     ) AS p(event, category, price) ON p.event = e.title
WHERE p.category IN (SELECT DISTINCT s.category FROM seats s WHERE s.venue_id = sh.venue_id);

COMMIT;

SELECT 'venues' AS what, count(*) FROM venues
UNION ALL SELECT 'seats', count(*) FROM seats
UNION ALL SELECT 'events (published)', count(*) FROM events WHERE status = 'PUBLISHED'
UNION ALL SELECT 'events (draft)', count(*) FROM events WHERE status = 'DRAFT'
UNION ALL SELECT 'shows', count(*) FROM shows
UNION ALL SELECT 'prices', count(*) FROM show_pricing;
