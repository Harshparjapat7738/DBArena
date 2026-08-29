package com.DBArena.common.events.serialization;

import com.DBArena.common.events.avro.OutboxDispatchedEvent;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

class AvroEventSerializerTest {

    @Test
    void roundTripsAnOutboxDispatchedEvent() {
        OutboxDispatchedEvent original = OutboxDispatchedEvent.newBuilder()
                .setEventId("01J000EVENT")
                .setEventType("problem.published")
                .setAggregateType("Problem")
                .setAggregateId("01J000PROBLEM")
                .setOccurredAtEpochMillis(1_700_000_000_000L)
                .setSchemaVersion(1)
                .setPayload(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}))
                .build();

        AvroEventSerializer<OutboxDispatchedEvent> serializer =
                new AvroEventSerializer<>(OutboxDispatchedEvent.class);

        byte[] bytes = serializer.serialize(original);
        OutboxDispatchedEvent decoded = serializer.deserialize(bytes);

        assertThat(decoded).isEqualTo(original);
    }
}
