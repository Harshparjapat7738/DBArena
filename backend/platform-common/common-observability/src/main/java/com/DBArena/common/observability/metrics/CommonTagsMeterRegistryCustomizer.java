package com.dbforge.common.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

/** Tags every metric emitted by a service with {@code service=<spring.application.name>}. */
public class CommonTagsMeterRegistryCustomizer implements MeterRegistryCustomizer<MeterRegistry> {

    private final String serviceName;

    public CommonTagsMeterRegistryCustomizer(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void customize(MeterRegistry registry) {
        registry.config().commonTags("service", serviceName);
    }
}
