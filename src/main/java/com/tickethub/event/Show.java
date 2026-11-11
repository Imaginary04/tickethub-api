package com.tickethub.event;

import com.tickethub.venue.SeatCategory;
import com.tickethub.venue.Venue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Prices belong to the show: saved, updated and deleted together with it
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowPricing> prices = new ArrayList<>();

    // Keeps both sides of the relationship in sync
    public void addPrice(SeatCategory category, BigDecimal price) {
        prices.add(new ShowPricing(this, category, price));
    }
}