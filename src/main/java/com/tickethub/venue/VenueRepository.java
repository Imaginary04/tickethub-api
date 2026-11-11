package com.tickethub.venue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    // Organizer dashboard: "my venues"
    List<Venue> findByOrganizerIdOrderByNameAsc(Long organizerId);
}