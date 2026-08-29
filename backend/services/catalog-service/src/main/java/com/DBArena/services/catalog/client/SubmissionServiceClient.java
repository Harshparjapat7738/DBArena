package com.DBArena.services.catalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/** Hard rule #2: OpenFeign, never a module dependency on submission-service. */
@FeignClient(name = "submission-service", url = "${dbarena.catalog.submission-service-uri}")
public interface SubmissionServiceClient {

    @GetMapping("/internal/v1/users/{userId}/problem-statuses")
    Map<String, String> getProblemStatuses(@PathVariable("userId") String userId);
}
