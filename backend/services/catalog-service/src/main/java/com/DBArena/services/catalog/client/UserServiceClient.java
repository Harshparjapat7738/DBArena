package com.DBArena.services.catalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Hard rule #2: OpenFeign, never a module dependency on user-service - same pattern as ai-assistant-service's CatalogServiceClient. */
@FeignClient(name = "user-service", url = "${dbarena.catalog.user-service-uri}")
public interface UserServiceClient {

    @GetMapping("/internal/v1/users/{userId}/bookmarked-slugs")
    List<String> getBookmarkedSlugs(@PathVariable("userId") String userId);
}
