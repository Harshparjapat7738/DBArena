package com.dbforge.common.events.outbox;

import com.dbforge.common.events.avro.OutboxDispatchedEvent;
import com.dbforge.common.events.serialization.EventSerializer;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Polls the outbox and publishes undispatched rows to Kafka in order,
 * stopping at the first failed send so ordering per aggregate is
 * preserved and the failed row is retried (not skipped) on the next
 * poll. Not scheduled by this class - batch size and interval are a
 * per-service concern; wire {@link #publishPending} to a
 * {@code @Scheduled} method in the owning service.
 */
public class OutboxRelay {

    private final OutboxRepository repository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final EventSerializer<OutboxDispatchedEvent> serializer;
    private final Function<OutboxRecord, String> topicResolver;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, byte[]> kafkaTemplate,
            EventSerializer<OutboxDispatchedEvent> serializer, Function<OutboxRecord, String> topicResolver) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.serializer = serializer;
        this.topicResolver = topicResolver;
    }

    /** Publishes up to {@code batchSize} undispatched rows. Returns how many were confirmed published. */
    public int publishPending(int batchSize) {
        List<OutboxRecord> pending = repository.findUndispatchedBatch(batchSize);
        List<String> dispatchedIds = new ArrayList<>();

        for (OutboxRecord record : pending) {
            try {
                OutboxDispatchedEvent envelope = OutboxEnvelopes.toEnvelope(record);
                byte[] payload = serializer.serialize(envelope);
                kafkaTemplate.send(topicResolver.apply(record), record.aggregateId(), payload).get();
                dispatchedIds.add(record.id());
            } catch (Exception e) {
                break;
            }
        }

        if (!dispatchedIds.isEmpty()) {
            repository.markDispatched(dispatchedIds);
        }
        return dispatchedIds.size();
    }
}
