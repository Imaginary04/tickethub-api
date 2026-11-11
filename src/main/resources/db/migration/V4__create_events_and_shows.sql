-- Lets a GiST index combine "=" on a normal column with "overlaps" on a time range
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE events (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organizer_id  BIGINT       NOT NULL REFERENCES users (id),
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    category      VARCHAR(20)  NOT NULL,
    poster_url    VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_events_category
        CHECK (category IN ('MOVIE', 'CONCERT', 'COMEDY', 'THEATRE', 'SPORTS', 'WORKSHOP', 'OTHER')),
    CONSTRAINT chk_events_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);

-- "My events" on the organizer dashboard
CREATE INDEX idx_events_organizer_id ON events (organizer_id);

-- Public listing: only PUBLISHED events, optionally one category
CREATE INDEX idx_events_status_category ON events (status, category);

CREATE TABLE shows (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id    BIGINT      NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    venue_id    BIGINT      NOT NULL REFERENCES venues (id),
    start_time  TIMESTAMPTZ NOT NULL,
    end_time    TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_shows_time_order
        CHECK (end_time > start_time),
    -- One venue can't host two shows at the same time.
    -- '[)' = start included, end excluded, so a show may start exactly when the previous one ends.
    CONSTRAINT ex_shows_venue_no_overlap
        EXCLUDE USING gist (venue_id WITH =, tstzrange(start_time, end_time, '[)') WITH &&)
);

-- Event page: upcoming shows of an event, in time order
CREATE INDEX idx_shows_event_id_start_time ON shows (event_id, start_time);

-- Listing filters: shows in a city (via venue) on a date
CREATE INDEX idx_shows_venue_id_start_time ON shows (venue_id, start_time);

CREATE TABLE show_pricing (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    show_id        BIGINT        NOT NULL REFERENCES shows (id) ON DELETE CASCADE,
    seat_category  VARCHAR(20)   NOT NULL,
    price          NUMERIC(10, 2) NOT NULL,

    CONSTRAINT chk_show_pricing_seat_category
        CHECK (seat_category IN ('SILVER', 'GOLD', 'PLATINUM')),
    CONSTRAINT chk_show_pricing_price
        CHECK (price >= 0),
    -- One price per category per show
    CONSTRAINT uq_show_pricing_show_category
        UNIQUE (show_id, seat_category)
);