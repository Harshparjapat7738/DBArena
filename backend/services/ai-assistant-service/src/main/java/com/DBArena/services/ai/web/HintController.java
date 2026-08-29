package com.DBArena.services.ai.web;

import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.security.web.CurrentUser;
import com.DBArena.services.ai.domain.HintCommand;
import com.DBArena.services.ai.ratelimit.HintRateLimiter;
import com.DBArena.services.ai.service.HintService;
import com.DBArena.services.ai.web.dto.HintRequestBody;
import com.DBArena.services.ai.web.dto.HintResponseBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Not in api-gateway's public-path allowlist (unlike catalog-service's
 * browsing endpoints) - every call here requires a valid access token,
 * both because an LLM call has a real cost per request and because
 * {@code @CurrentUser} gives every hint a resolvable requester for
 * {@link com.DBArena.services.ai.ratelimit.HintRateLimiter}, which this
 * method enforces before doing any of the expensive work below (added in
 * the audit pass that closed M16's "no rate limiting" carried-forward
 * gap).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class HintController {

    private final HintService hintService;
    private final HintRateLimiter rateLimiter;

    public HintController(HintService hintService, HintRateLimiter rateLimiter) {
        this.hintService = hintService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/problems/{slug}/hint")
    public HintResponseBody hint(
            @PathVariable String slug,
            @Valid @RequestBody HintRequestBody request,
            @CurrentUser AuthenticatedUser user) {
        rateLimiter.checkAndRecord(user.userId().value());
        HintCommand command = request.toCommand(slug);
        return HintResponseBody.from(hintService.generateHint(command));
    }
}
