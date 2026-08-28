package com.dbforge.services.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The hard-rule-#2 way to read another service's data: OpenFeign, never a
 * direct module dependency on catalog-service. Calls catalog-service
 * directly on its own base URL (not through api-gateway) - this is an
 * internal, service-to-service call, not a browser-facing one; the
 * gateway's route table and public-path allowlist are for external
 * clients.
 */
@FeignClient(name = "catalog-service", url = "${dbforge.ai.catalog-service-uri}")
public interface CatalogServiceClient {

    @GetMapping("/api/v1/catalog/problems/{slug}")
    CatalogProblemResponse getProblem(@PathVariable("slug") String slug);
}
