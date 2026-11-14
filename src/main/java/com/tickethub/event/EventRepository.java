package com.tickethub.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// JpaSpecificationExecutor adds findAll(Specification, Pageable) for the public search
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    // Organizer dashboard: "my events", newest first
    List<Event> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId);
}