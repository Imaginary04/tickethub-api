package com.tickethub.event;

import com.tickethub.common.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Put on any method that changes what the public sees for an event.
// The method must have a parameter named "eventId".
// Clears that event's page and every cached search (any search could contain it).
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS, key = "#eventId"),
        @CacheEvict(cacheNames = CacheConfig.EVENT_SEARCH, allEntries = true)
})
public @interface EvictPublicEventCaches {
}