package com.rideshare.platform.route.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * FR: Section 7 Route Management - "Build Redis Index":
 *   H3 Cell -> Ride IDs   (Redis Set, key = "route_index:{h3Cell}")
 * This is what powers sub-300ms ride search (Section 24 Performance Requirements).
 */
@Service
@RequiredArgsConstructor
public class RouteRedisIndexService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "route_index:";
    private static final long TTL_DAYS = 3; // rides beyond this are cleaned up by the ride-completion job

    /** Indexes every cell in one pipelined round trip instead of one SADD+EXPIRE pair per cell. */
    public void indexCells(Set<String> h3Cells, Long rideId) {
        if (h3Cells.isEmpty()) return;
        String member = rideId.toString();
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) {
                for (String cell : h3Cells) {
                    String key = KEY_PREFIX + cell;
                    operations.opsForSet().add(key, member);
                    operations.expire(key, TTL_DAYS, TimeUnit.DAYS);
                }
                return null;
            }
        });
    }

    public void removeCellsForRide(Set<String> h3Cells, Long rideId) {
        if (h3Cells.isEmpty()) return;
        String member = rideId.toString();
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) {
                for (String cell : h3Cells) {
                    operations.opsForSet().remove(KEY_PREFIX + cell, member);
                }
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public Set<String> ridesInCell(String h3Cell) {
        Set<Object> members = redisTemplate.opsForSet().members(KEY_PREFIX + h3Cell);
        return members == null ? Set.of() : (Set<String>) (Set<?>) members;
    }
}
