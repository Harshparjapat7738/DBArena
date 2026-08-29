package com.DBArena.services.catalog.web.dto.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "must be lowercase-kebab-case") String slug,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description) {

    public CreateTopicRequest {
        description = description == null ? "" : description;
    }
}
