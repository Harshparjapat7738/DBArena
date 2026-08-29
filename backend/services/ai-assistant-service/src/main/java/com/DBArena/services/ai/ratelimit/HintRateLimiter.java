package com.dbforge.services.ai.ratelimit;

import com.dbforge.services.ai.config.AiProviderProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fixed-window per-user rate limit on the hint endpoint. Closes a gap
 * M16's Session Log flagged explicitly: "no rate limiting or per-user
 * cost control on the hint endpoint... this needs addressing before any
 * real deployment, not just before scale" - an LLM call has a real cost
 * per request.
 *
 * <p>In-memory and single-instance-scoped, deliberately: there is no
 * Redis wiring anywhere in this reactor yet (no compose/deploy config
 * exists to add it to - {@code deploy/} isn't created until B19), and
 * this service runs as exactly one replica today. Swap for a
 * Redis-backed limiter (INCR + EXPIRE) before ai-assistant-service is
 * ever run as more than one instance, or the limit becomes per-instance
 * instead of per-user.
 */
@Component
public class HintRateLimiter {

    private final int maxRequestsPerWindow;
    private final Duration window = Duration.ofHours(1);
    private final Clock clock;
    private final ConcurrentHashMap<String, AtomicReference<Window>> windowsByUser = new ConcurrentHashMap<>();

    public HintRateLimiter(AiProviderProperties properties, Clock clock) {
        this.maxRequestsPerWindow = properties.getHintRateLimitPerHour();
        this.clock = clock;
    }

    /** Throws {@link HintRateLimitExceededException} if the caller is over budget; otherwise records this call. */
    public void checkAndRecord(String userId) {
        Instant now = clock.instant();
        AtomicReference<Window> ref = windowsByUser.computeIfAbsent(userId,
                id -> new AtomicReference<>(new Window(now, 0)));

        while (true) {
            Window current = ref.get();
            Window effective = current.resetIfWindowElapsed(now, window);
            if (effective.count() >= maxRequestsPerWindow) {
                throw new HintRateLimitExceededException(maxRequestsPerWindow, window);
            }
            if (ref.compareAndSet(current, effective.increment())) {
                return;
            }
            // Lost the CAS race to a concurrent request from the same user - retry against the fresh value.
        }
    }

    private record Window(Instant windowStart, int count) {
        Window resetIfWindowElapsed(Instant now, Duration window) {
            return now.isAfter(windowStart.plus(window)) ? new Window(now, 0) : this;
        }

        Window increment() {
            return new Window(windowStart, count + 1);
        }
    }
}
