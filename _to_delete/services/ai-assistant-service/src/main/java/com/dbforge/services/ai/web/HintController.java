package com.dbforge.services.ai.web;

import com.dbforge.common.security.AuthenticatedUser;
import com.dbforge.common.security.web.CurrentUser;
import com.dbforge.services.ai.domain.HintCommand;
import com.dbforge.services.ai.service.HintService;
import com.dbforge.services.ai.web.dto.HintRequestBody;
import com.dbforge.services.ai.web.dto.HintResponseBody;
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
 * {@code @CurrentUser} gives every hint a resolvable requester for the
 * observability/rate-limiting this milestone doesn't build yet but a
 * later one will want (see Carried forward).
 */
@RestController
@RequestMapping("/api/v1/ai")
public class HintController {

    private final HintService hintService;

    public HintController(HintService hintService) {
        this.hintService = hintService;
    }

    @PostMapping("/problems/{slug}/hint")
    public HintResponseBody hint(
            @PathVariable String slug,
            @Valid @RequestBody HintRequestBody request,
            @CurrentUser AuthenticatedUser user) {
        HintCommand command = request.toCommand(slug);
        return HintResponseBody.from(hintService.generateHint(command));
    }
}
