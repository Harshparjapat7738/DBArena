package com.DBArena.common.events.idempotency;

/**
 * Enforces root CLAUDE.md hard rule: "every Kafka consumer must be
 * idempotent, keyed on an event or submission id with a UNIQUE
 * constraint doing the work". Implementations back this with a table
 * carrying a {@code UNIQUE(event_id, consumer_group)} constraint and let
 * the constraint violation - not a check-then-insert race - decide the
 * answer, so it stays correct under concurrent consumer instances.
 */
public interface ProcessedEventGuard {

    /**
     * Attempts to atomically claim {@code (eventId, consumerGroup)}.
     * Returns {@code false} if it was already claimed - the caller must
     * then skip reprocessing rather than redo side effects.
     */
    boolean tryMarkProcessed(String eventId, String consumerGroup);
}
