package com.DBArena.services.user.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBookmarkRequest(@NotBlank String problemSlug) {
}
