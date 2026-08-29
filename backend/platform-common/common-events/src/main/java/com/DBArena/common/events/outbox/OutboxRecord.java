package com.DBArena.common.events.outbox;

import java.time.Instant;

/**
 * One row of the transactional outbox table. The owning service inserts
 * this in the same database transaction as the business write it
 * describes (root CLAUDE.md hard rule: "every producer writes through
 * the transactional outbox"), then a poller drains undispatched rows via
 * {@link OutboxRelay}.
 */
public record OutboxRecord(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        byte[] payload,
        Instant occurredAt,
        boolean dispatched) {

    public OutboxRecord {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
    }

    public static OutboxRecord pending(String id, String aggregateType, String aggregateId, String eventType,
            byte[] payload, Instant occurredAt) {
        return new OutboxRecord(id, aggregateType, aggregateId, eventType, payload, occurredAt, false);
    }
}
