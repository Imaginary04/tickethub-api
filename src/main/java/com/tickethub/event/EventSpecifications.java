package com.tickethub.event;

import com.tickethub.venue.Venue;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Small, reusable WHERE-clause pieces for the public event search.
// The service combines only the ones the user actually asked for.
public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), EventStatus.PUBLISHED);
    }

    public static Specification<Event> hasCategory(EventCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    // Case-insensitive "contains"; % and _ typed by the user are matched literally
    public static Specification<Event> titleContains(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\');
    }

    // Keeps events that have at least one show in [from, to) (in the city, if given),
    // and sorts them by their earliest such show: soonest first.
    public static Specification<Event> hasShowBetween(Instant from, Instant to, String cityKey) {
        return (root, query, cb) -> {
            // The page query gets the sort; the "count(*)" query that Spring runs for paging doesn't need it
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                Subquery<Instant> soonest = query.subquery(Instant.class);
                Root<Show> show = soonest.from(Show.class);
                soonest.select(cb.least(show.<Instant>get("startTime")))
                        .where(showMatches(cb, show, root, from, to, cityKey));
                query.orderBy(cb.asc(soonest), cb.asc(root.get("id")));
            }

            // WHERE EXISTS (SELECT 1 FROM shows s ... WHERE s.event_id = e.id AND ...)
            Subquery<Long> any = query.subquery(Long.class);
            Root<Show> show = any.from(Show.class);
            any.select(show.get("id")).where(showMatches(cb, show, root, from, to, cityKey));
            return cb.exists(any);
        };
    }

    private static Predicate[] showMatches(CriteriaBuilder cb, Root<Show> show, Root<Event> event,
                                           Instant from, Instant to, String cityKey) {
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(cb.equal(show.get("event"), event));
        conditions.add(cb.greaterThanOrEqualTo(show.get("startTime"), from));
        conditions.add(cb.lessThan(show.get("startTime"), to));
        if (!cityKey.isEmpty()) {
            Join<Show, Venue> venue = show.join("venue");
            // lower(city) = ? uses the idx_venues_city index from V3
            conditions.add(cb.equal(cb.lower(venue.get("city")), cityKey));
        }
        return conditions.toArray(Predicate[]::new);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}