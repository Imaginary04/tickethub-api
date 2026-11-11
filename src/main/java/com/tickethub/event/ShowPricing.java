package com.tickethub.event;

import com.tickethub.venue.SeatCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "show_pricing")
@Getter
@Setter
@NoArgsConstructor
public class ShowPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_category", nullable = false, length = 20)
    private SeatCategory seatCategory;

    // Money: BigDecimal (exact), never double
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    public ShowPricing(Show show, SeatCategory seatCategory, BigDecimal price) {
        this.show = show;
        this.seatCategory = seatCategory;
        this.price = price;
    }
}