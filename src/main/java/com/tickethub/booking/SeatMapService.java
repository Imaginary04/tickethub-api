package com.tickethub.booking;

import com.tickethub.booking.dto.SeatMapResponse;
import com.tickethub.booking.dto.SeatMapResponse.Price;
import com.tickethub.booking.dto.SeatMapResponse.Row;
import com.tickethub.booking.dto.SeatMapResponse.SeatInfo;
import com.tickethub.common.exception.ResourceNotFoundException;
import com.tickethub.event.EventStatus;
import com.tickethub.event.Show;
import com.tickethub.event.ShowPricing;
import com.tickethub.event.ShowRepository;
import com.tickethub.venue.Seat;
import com.tickethub.venue.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatMapService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;

    // 3 queries in total, however many seats: the show (+ event, venue, prices), the seats, the taken seats.
    // Not cached: seats change every few seconds while people book.
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(Long showId) {
        // Only bookable shows exist for customers: published event, not started yet
        Show show = showRepository.findWithDetailsById(showId)
                .filter(s -> s.getEvent().getStatus() == EventStatus.PUBLISHED)
                .filter(s -> s.getStartTime().isAfter(Instant.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Show " + showId + " not found"));

        Map<Long, BookingStatus> taken = bookingSeatRepository.findActiveSeatsByShowId(showId).stream()
                .collect(Collectors.toMap(ActiveSeat::seatId, ActiveSeat::status));

        // Seats arrive sorted (A1, A2 ... B1), so grouping keeps the row order
        Map<String, List<SeatInfo>> rows = new LinkedHashMap<>();
        int available = 0;
        for (Seat seat : seatRepository.findByVenueIdOrderByRowLabelAscSeatNumberAsc(show.getVenue().getId())) {
            SeatStatus status = statusOf(taken.get(seat.getId()));
            if (status == SeatStatus.AVAILABLE) {
                available++;
            }
            rows.computeIfAbsent(seat.getRowLabel(), label -> new ArrayList<>())
                    .add(new SeatInfo(seat.getId(), seat.getSeatNumber(), seat.getCategory(), status));
        }

        List<Price> prices = show.getPrices().stream()
                .sorted(Comparator.comparing(ShowPricing::getSeatCategory))
                .map(p -> new Price(p.getSeatCategory(), p.getPrice()))
                .toList();

        return new SeatMapResponse(
                show.getId(),
                show.getEvent().getId(),
                show.getEvent().getTitle(),
                show.getVenue().getName(),
                show.getVenue().getCity(),
                show.getStartTime(),
                show.getEndTime(),
                prices,
                rows.entrySet().stream().map(row -> new Row(row.getKey(), row.getValue())).toList(),
                available);
    }

    // No active booking row = available; a PENDING booking = held; a CONFIRMED booking = booked
    private static SeatStatus statusOf(BookingStatus bookingStatus) {
        if (bookingStatus == null) {
            return SeatStatus.AVAILABLE;
        }
        return bookingStatus == BookingStatus.CONFIRMED ? SeatStatus.BOOKED : SeatStatus.HELD;
    }
}