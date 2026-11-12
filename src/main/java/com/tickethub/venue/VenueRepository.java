package com.tickethub.venue;

import com.tickethub.venue.dto.VenueResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    // Organizer dashboard: "my venues" with seat counts in ONE query (no N+1 counting per venue)
    @Query("""
            select new com.tickethub.venue.dto.VenueResponse(
                v.id, v.name, v.city, v.address, count(s), v.createdAt)
            from Venue v
            left join Seat s on s.venue = v
            where v.organizer.id = :organizerId
            group by v.id, v.name, v.city, v.address, v.createdAt
            order by v.name
            """)
    List<VenueResponse> findSummariesByOrganizerId(@Param("organizerId") Long organizerId);
}