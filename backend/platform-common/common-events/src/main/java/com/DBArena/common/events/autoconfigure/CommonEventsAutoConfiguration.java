package com.dbforge.common.events.autoconfigure;

import com.dbforge.common.events.avro.OutboxDispatchedEvent;
import com.dbforge.common.events.serialization.AvroEventSerializer;
import com.dbforge.common.events.serialization.EventSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * Forces every producer in the platform to be idempotent (hard rule: no
 * silent at-least-once duplication on the producer side) and provides the
 * default envelope serializer. Activates only when spring-kafka is on the
 * classpath, so common-events stays a no-op dependency for services that
 * don't touch Kafka.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
public class CommonEventsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DefaultKafkaProducerFactoryCustomizer idempotentProducerCustomizer() {
        return producerFactory -> producerFactory.updateConfigs(Map.of(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.ACKS_CONFIG, "all"));
    }

    @Bean
    @ConditionalOnMissingBean
    public EventSerializer<OutboxDispatchedEvent> outboxDispatchedEventSerializer() {
        return new AvroEventSerializer<>(OutboxDispatchedEvent.class);
    }
}
