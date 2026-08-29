package com.DBArena.services.execution.web;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.security.web.CurrentUser;
import com.DBArena.services.execution.domain.Execution;
import com.DBArena.services.execution.service.ExecutionService;
import com.DBArena.services.execution.web.dto.CreateExecutionRequest;
import com.DBArena.services.execution.web.dto.ExecutionResponse;
import com.DBArena.services.execution.web.dto.ExplainResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Every endpoint here requires an authenticated caller ({@code @CurrentUser}
 * without {@code Optional} - see common-security's {@code CurrentUserArgumentResolver},
 * which 401s outright when absent) - unlike catalog-service's public browsing
 * endpoints, nothing here is reachable anonymously; api-gateway's
 * {@code PublicPaths} carries no entry for this prefix (see the B04 Session
 * Log entry).
 */
@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecutionResponse create(@CurrentUser AuthenticatedUser user, @Valid @RequestBody CreateExecutionRequest request) {
        Execution execution = executionService.create(
                user, request.engine(), request.datasetSlug(), Optional.ofNullable(request.problemSlug()), request.statementText());
        return ExecutionResponse.from(execution);
    }

    @GetMapping("/{id}")
    public ExecutionResponse get(@CurrentUser AuthenticatedUser user, @PathVariable String id) {
        return ExecutionResponse.from(executionService.getOwned(user, TypedId.of(id)));
    }

    @GetMapping
    public CursorPage<ExecutionResponse> list(
            @CurrentUser AuthenticatedUser user,
            @RequestParam Optional<String> cursor,
            @RequestParam Optional<Integer> limit) {
        PageRequest pageRequest = new PageRequest(limit.orElse(PageRequest.DEFAULT_LIMIT), cursor);
        return executionService.listOwned(user, pageRequest).map(ExecutionResponse::from);
    }

    @PostMapping("/{id}/cancel")
    public ExecutionResponse cancel(@CurrentUser AuthenticatedUser user, @PathVariable String id) {
        return ExecutionResponse.from(executionService.cancel(user, TypedId.of(id)));
    }

    @PostMapping("/{id}/explain")
    public ExplainResponse explain(@CurrentUser AuthenticatedUser user, @PathVariable String id) {
        return ExplainResponse.from(executionService.explain(user, TypedId.of(id)));
    }
}
