package com.dbforge.common.events.outbox;

import com.dbforge.common.events.avro.OutboxDispatchedEvent;
import com.dbforge.common.events.serialization.EventSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxRelayTest {

    @SuppressWarnings("unchecked")
    @Test
    void publishesUndispatchedRowsAndMarksThemDispatched() throws Exception {
        OutboxRepository repository = mock(OutboxRepository.class);
        KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
        EventSerializer<OutboxDispatchedEvent> serializer = mock(EventSerializer.class);

        OutboxRecord record = OutboxRecord.pending(
                "01J000EVENT", "Problem", "01J000PROBLEM", "problem.published",
                new byte[]{9}, Instant.parse("2026-01-01T00:00:00Z"));

        when(repository.findUndispatchedBatch(10)).thenReturn(List.of(record));
        when(serializer.serialize(any())).thenReturn(new byte[]{1, 2, 3});

        SendResult<String, byte[]> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(eq("problem-events"), eq("01J000PROBLEM"), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        OutboxRelay relay = new OutboxRelay(repository, kafkaTemplate, serializer, r -> "problem-events");

        int published = relay.publishPending(10);

        assertThat(published).isEqualTo(1);
        ArgumentCaptor<List<String>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).markDispatched(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly("01J000EVENT");
    }

    @SuppressWarnings("unchecked")
    @Test
    void stopsAtFirstFailureAndLeavesItForRetry() throws Exception {
        OutboxRepository repository = mock(OutboxRepository.class);
        KafkaTemplate<String, byte[]> kafkaTemplate = mock(KafkaTemplate.class);
        EventSerializer<OutboxDispatchedEvent> serializer = mock(EventSerializer.class);

        OutboxRecord failing = OutboxRecord.pending(
                "01J000FAIL", "Problem", "01J000PROBLEM", "problem.published",
                new byte[]{9}, Instant.parse("2026-01-01T00:00:00Z"));

        when(repository.findUndispatchedBatch(10)).thenReturn(List.of(failing));
        when(serializer.serialize(any())).thenReturn(new byte[]{1});

        CompletableFuture<SendResult<String, byte[]>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any(byte[].class))).thenReturn(failed);

        OutboxRelay relay = new OutboxRelay(repository, kafkaTemplate, serializer, r -> "problem-events");

        int published = relay.publishPending(10);

        assertThat(published).isZero();
        verify(repository, never()).markDispatched(any());
    }
}
