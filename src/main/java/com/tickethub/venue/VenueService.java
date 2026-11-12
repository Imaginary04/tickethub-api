package com.tickethub.venue;

import com.tickethub.auth.AuthenticatedUser;
import com.tickethub.common.exception.ConflictException;
import com.tickethub.common.exception.ResourceNotFoundException;
import com.tickethub.event.ShowRepository;
import com.tickethub.user.Role;
import com.tickethub.user.UserRepository;
import com.tickethub.venue.dto.CreateVenueRequest;
import com.tickethub.venue.dto.RowRequest;
import com.tickethub.venue.dto.VenueDetailsResponse;
import com.tickethub.venue.dto.VenueDetailsResponse.RowResponse;
import com.tickethub.venue.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VenueService {

    private static final String INSERT_SEAT_SQL =
            "INSERT INTO seats (venue_id, row_label, seat_number, category) VALUES (?, ?, ?, ?)";

    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public VenueResponse create(CreateVenueRequest request, AuthenticatedUser currentUser) {
        Venue venue = new Venue();
        // A reference only (no SELECT): we just need the id for organizer_id
        venue.setOrganizer(userRepository.getReferenceById(currentUser.id()));
        venue.setName(request.name().trim());
        venue.setCity(request.city().trim());
        venue.setAddress(request.address().trim());
        venueRepository.saveAndFlush(venue);

        List<Seat> seats = buildLayout(venue, request.rows());
        insertSeats(venue.getId(), seats);

        return new VenueResponse(venue.getId(), venue.getName(), venue.getCity(),
                venue.getAddress(), seats.size(), venue.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> listMine(AuthenticatedUser currentUser) {
        return venueRepository.findSummariesByOrganizerId(currentUser.id());
    }

    @Transactional(readOnly = true)
    public VenueDetailsResponse get(Long venueId, AuthenticatedUser currentUser) {
        Venue venue = findOwnedVenue(venueId, currentUser);
        List<Seat> seats = seatRepository.findByVenueIdOrderByRowLabelAscSeatNumberAsc(venueId);

        // Seats come sorted by row, so grouping keeps the A, B, C... order
        Map<String, List<Seat>> byRow = new LinkedHashMap<>();
        for (Seat seat : seats) {
            byRow.computeIfAbsent(seat.getRowLabel(), label -> new ArrayList<>()).add(seat);
        }
        List<RowResponse> rows = byRow.entrySet().stream()
                .map(row -> new RowResponse(row.getKey(), row.getValue().getFirst().getCategory(), row.getValue().size()))
                .toList();

        return new VenueDetailsResponse(venue.getId(), venue.getName(), venue.getCity(),
                venue.getAddress(), seats.size(), venue.getCreatedAt(), rows);
    }

    @Transactional
    public void delete(Long venueId, AuthenticatedUser currentUser) {
        Venue venue = findOwnedVenue(venueId, currentUser);
        // Friendly message here; the foreign key in V4 is the real safety net
        if (showRepository.existsByVenueId(venueId)) {
            throw new ConflictException("Venue has shows and cannot be deleted");
        }
        venueRepository.delete(venue); // seats go too: ON DELETE CASCADE
    }

    // Not yours = 404, not 403: don't reveal that someone else's venue exists
    private Venue findOwnedVenue(Long venueId, AuthenticatedUser currentUser) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue " + venueId + " not found"));
        boolean isOwner = venue.getOrganizer().getId().equals(currentUser.id());
        if (!isOwner && currentUser.role() != Role.ADMIN) {
            throw new ResourceNotFoundException("Venue " + venueId + " not found");
        }
        return venue;
    }

    private List<Seat> buildLayout(Venue venue, List<RowRequest> rows) {
        List<Seat> seats = new ArrayList<>();
        for (RowRequest row : rows) {
            for (int number = 1; number <= row.seatCount(); number++) {
                seats.add(new Seat(venue, row.label(), number, row.category()));
            }
        }
        return seats;
    }

    // JDBC batch: sends the seats to Postgres in groups of 500 instead of one INSERT per round trip.
    // (Hibernate can't batch inserts for IDENTITY ids, because it needs each new id back immediately.)
    private void insertSeats(Long venueId, List<Seat> seats) {
        jdbcTemplate.batchUpdate(INSERT_SEAT_SQL, seats, 500, (ps, seat) -> {
            ps.setLong(1, venueId);
            ps.setString(2, seat.getRowLabel());
            ps.setInt(3, seat.getSeatNumber());
            ps.setString(4, seat.getCategory().name());
        });
    }
}