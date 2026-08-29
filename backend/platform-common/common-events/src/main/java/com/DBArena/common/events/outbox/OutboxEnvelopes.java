package com.DBArena.common.events.outbox;

import com.DBArena.common.events.avro.OutboxDispatchedEvent;

import java.nio.ByteBuffer;

/** Builds the wire envelope ({@code src/main/avro/OutboxDispatchedEvent.avsc}) from an {@link OutboxRecord}. */
public final class OutboxEnvelopes {

    private OutboxEnvelopes() {
    }

    public static OutboxDispatchedEvent toEnvelope(OutboxRecord record) {
        return OutboxDispatchedEvent.newBuilder()
                .setEventId(record.id())
                .setEventType(record.eventType())
                .setAggregateType(record.aggregateType())
                .setAggregateId(record.aggregateId())
                .setOccurredAtEpochMillis(record.occurredAt().toEpochMilli())
                .setSchemaVersion(1)
                .setPayload(ByteBuffer.wrap(record.payload()))
                .build();
    }
}
