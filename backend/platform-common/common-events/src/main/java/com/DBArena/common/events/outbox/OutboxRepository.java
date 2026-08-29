package com.DBArena.common.events.outbox;

import java.util.List;

/**
 * Contract for the outbox table. Each service implements this against
 * whatever persistence technology it already uses (JPA, jOOQ, plain
 * JDBC) - common-events does not pick an ORM. {@link #save} must be
 * called inside the same transaction as the business write it
 * accompanies.
 */
public interface OutboxRepository {

    void save(OutboxRecord record);

    List<OutboxRecord> findUndispatchedBatch(int limit);

    void markDispatched(List<String> ids);
}
