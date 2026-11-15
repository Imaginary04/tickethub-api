CREATE TABLE bookings (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users (id),
    show_id          BIGINT         NOT NULL REFERENCES shows (id),
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(10, 2) NOT NULL,
    hold_expires_at  TIMESTAMPTZ,
    -- Optimistic locking (@Version): two updates of the same booking can't silently overwrite each other
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT chk_bookings_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_bookings_total_amount
        CHECK (total_amount >= 0),
    -- A seat hold always has a deadline
    CONSTRAINT chk_bookings_pending_has_expiry
        CHECK (status <> 'PENDING' OR hold_expires_at IS NOT NULL),
    -- Lets booking_seats reference (id, show_id) together (see below)
    CONSTRAINT uq_bookings_id_show UNIQUE (id, show_id)
);

-- "My bookings", newest first
CREATE INDEX idx_bookings_user_id_created_at ON bookings (user_id, created_at DESC);

-- Organizer sales per show, and "does this show have bookings?"
CREATE INDEX idx_bookings_show_id ON bookings (show_id);

-- The cleanup job: "PENDING holds whose deadline has passed".
-- Partial index: only PENDING rows are in it, so it stays small however many bookings exist.
CREATE INDEX idx_bookings_pending_expiry ON bookings (hold_expires_at) WHERE status = 'PENDING';

CREATE TABLE booking_seats (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id  BIGINT         NOT NULL,
    show_id     BIGINT         NOT NULL,
    seat_id     BIGINT         NOT NULL REFERENCES seats (id),
    -- Price when booked: later price changes don't affect existing bookings
    price       NUMERIC(10, 2) NOT NULL,
    -- true while the booking holds or owns the seat; false once it expires or is cancelled
    active      BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_booking_seats_price
        CHECK (price >= 0),
    -- The seat's show must be the booking's show
    CONSTRAINT fk_booking_seats_booking
        FOREIGN KEY (booking_id, show_id) REFERENCES bookings (id, show_id) ON DELETE CASCADE
);

-- THE double-booking guard: a seat can be in only ONE active booking per show.
-- Expired or cancelled rows (active = false) don't count, so the seat can be booked again.
CREATE UNIQUE INDEX uq_booking_seats_active_seat ON booking_seats (show_id, seat_id) WHERE active;

-- "Seats of this booking" (checkout page, releasing a hold)
CREATE INDEX idx_booking_seats_booking_id ON booking_seats (booking_id);