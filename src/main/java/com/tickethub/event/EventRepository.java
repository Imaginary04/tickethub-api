package com.tickethub.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Organizer dashboard: "my events", newest first
    List<Event> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId);
}