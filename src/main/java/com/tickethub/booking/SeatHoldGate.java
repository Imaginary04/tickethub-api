package com.tickethub.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

// The fast front gate: holds seats in Redis with a time limit.
// Postgres (the unique index on booking_seats) is the real guarantee, so if Redis is down we just skip the gate.
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldGate {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final RedisScript<List<Long>> HOLD_SCRIPT =
            (RedisScript) RedisScript.of(new ClassPathResource("redis/hold-seats.lua"), List.class);

    private final StringRedisTemplate redis;

    // Returns the seat ids that someone else already holds; an empty list means we now hold all of them
    public List<Long> tryHold(Long showId, List<Long> seatIds, Duration ttl, String holder) {
        try {
            List<Long> takenPositions = redis.execute(HOLD_SCRIPT, keys(showId, seatIds), holder, String.valueOf(ttl.toMillis()));
            return takenPositions.stream().map(position -> seatIds.get(position.intValue() - 1)).toList();
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable, skipping the seat-hold gate: {}", ex.getMessage());
            return List.of();
        }
    }

    public void release(Long showId, Collection<Long> seatIds) {
        try {
            redis.delete(keys(showId, seatIds));
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable, holds will expire on their own: {}", ex.getMessage());
        }
    }

    // {showId} in braces is a Redis Cluster "hash tag": all seats of one show land on the same node,
    // which a multi-key script needs. On a single Redis it changes nothing.
    private static List<String> keys(Long showId, Collection<Long> seatIds) {
        return seatIds.stream().map(seatId -> "tickethub:hold:{" + showId + "}:" + seatId).toList();
    }
}