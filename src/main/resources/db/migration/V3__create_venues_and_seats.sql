CREATE TABLE venues (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organizer_id  BIGINT       NOT NULL REFERENCES users (id),
    name          VARCHAR(150) NOT NULL,
    city          VARCHAR(100) NOT NULL,
    address       VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- "My venues" on the organizer dashboard
CREATE INDEX idx_venues_organizer_id ON venues (organizer_id);

-- Filter events by city, case-insensitive (Mumbai = mumbai)
CREATE INDEX idx_venues_city ON venues (LOWER(city));

CREATE TABLE seats (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id     BIGINT      NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    row_label    VARCHAR(5)  NOT NULL,
    seat_number  INT         NOT NULL,
    category     VARCHAR(20) NOT NULL,

    CONSTRAINT chk_seats_category
        CHECK (category IN ('SILVER', 'GOLD', 'PLATINUM')),
    CONSTRAINT chk_seats_seat_number
        CHECK (seat_number > 0),
    -- No two "A-5" seats in the same venue.
    -- Its index starts with venue_id, so it also speeds up "all seats of a venue".
    CONSTRAINT uq_seats_venue_row_number
        UNIQUE (venue_id, row_label, seat_number)
);