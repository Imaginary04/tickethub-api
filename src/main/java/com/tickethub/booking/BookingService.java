package com.tickethub.booking;

import com.tickethub.auth.AuthenticatedUser;
import com.tickethub.booking.dto.HoldRequest;
import com.tickethub.booking.dto.HoldResponse;
import com.tickethub.booking.dto.HoldResponse.HeldSeat;
import com.tickethub.common.exception.BadRequestException;
import com.tickethub.common.exception.ConflictException;
import com.tickethub.common.exception.ResourceNotFoundException;
import com.tickethub.event.EventStatus;
import com.tickethub.event.Show;
import com.tickethub.event.ShowPricing;
import com.tickethub.event.ShowRepository;
import com.tickethub.user.UserRepository;
import com.tickethub.venue.Seat;
import com.tickethub.venue.SeatCategory;
import com.tickethub.venue.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final SeatHoldGate seatHoldGate;
    private final BookingProperties properties;
    private final TransactionTemplate transactionTemplate;

    // Not @Transactional on purpose: Redis isn't part of the database transaction,
    // so the steps are run one by one, each database step in its own short transaction.
    public HoldResponse hold(Long showId, HoldRequest request, AuthenticatedUser currentUser) {
        List<Long> seatIds = request.seatIds().stream().sorted().toList();
        if (seatIds.size() > properties.maxSeatsPerHold()) {
            throw new BadRequestException("You can hold at most " + properties.maxSeatsPerHold() + " seats at a time");
        }

        // 1. Checks that only read (show, seats, prices, existing hold)
        List<HeldSeat> lines = transactionTemplate.execute(status -> checkAndPrice(showId, seatIds, currentUser));

        // 2. Fast gate: Redis holds ALL the seats or NONE
        List<Long> taken = seatHoldGate.tryHold(showId, seatIds, properties.holdDuration(), "user:" + currentUser.id());
        if (!taken.isEmpty()) {
            throw new ConflictException("These seats were just taken: " + labels(lines, taken));
        }

        // 3. The guarantee: the unique index on booking_seats. If it refuses, give the Redis seats back.
        try {
            Booking booking = transactionTemplate.execute(status -> saveBooking(showId, lines, currentUser));
            return new HoldResponse(booking.getId(), showId, booking.getStatus(), booking.getTotalAmount(),
                    booking.getHoldExpiresAt(), lines);
        } catch (DataIntegrityViolationException ex) {
            seatHoldGate.release(showId, seatIds);
            throw new ConflictException("These seats were just taken. Please choose others.");
        } catch (RuntimeException ex) {
            seatHoldGate.release(showId, seatIds);
            throw ex;
        }
    }

    private List<HeldSeat> checkAndPrice(Long showId, List<Long> seatIds, AuthenticatedUser currentUser) {
        // Same rule as the seat map: only published, not-yet-started shows can be booked
        Show show = showRepository.findWithDetailsById(showId)
                .filter(s -> s.getEvent().getStatus() == EventStatus.PUBLISHED)
                .filter(s -> s.getStartTime().isAfter(Instant.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " not found"));

        if (bookingRepository.existsByUserIdAndShowIdAndStatus(currentUser.id(), showId, BookingStatus.PENDING)) {
            throw new ConflictException("You already have seats on hold for this show");
        }

        // Every seat must exist and be in this show's venue
        List<Seat> seats = seatRepository.findAllById(seatIds);
        Long venueId = show.getVenue().getId();
        if (seats.size() != seatIds.size() || seats.stream().anyMatch(s -> !s.getVenue().getId().equals(venueId))) {
            throw new BadRequestException("Some of these seats don't belong to this show");
        }

        Map<SeatCategory, BigDecimal> prices = show.getPrices().stream()
                .collect(Collectors.toMap(ShowPricing::getSeatCategory, ShowPricing::getPrice));

        return seats.stream()
                .sorted(Comparator.comparing(Seat::getRowLabel).thenComparing(Seat::getSeatNumber))
                .map(s -> new HeldSeat(s.getId(), s.getRowLabel(), s.getSeatNumber(), s.getCategory(), prices.get(s.getCategory())))
                .toList();
    }

    private Booking saveBooking(Long showId, List<HeldSeat> lines, AuthenticatedUser currentUser) {
        Booking booking = new Booking();
        booking.setUser(userRepository.getReferenceById(currentUser.id()));
        booking.setShow(showRepository.getReferenceById(showId));
        booking.setHoldExpiresAt(Instant.now().plus(properties.holdDuration()));
        booking.setTotalAmount(lines.stream().map(HeldSeat::price).reduce(BigDecimal.ZERO, BigDecimal::add));
        for (HeldSeat line : lines) {
            booking.addSeat(seatRepository.getReferenceById(line.seatId()), line.price());
        }
        return bookingRepository.saveAndFlush(booking);
    }

    // [31, 32] -> "A1, A2"
    private static String labels(List<HeldSeat> lines, List<Long> seatIds) {
        return lines.stream()
                .filter(line -> seatIds.contains(line.seatId()))
                .map(line -> line.row() + line.number())
                .collect(Collectors.joining(", "));
    }
}