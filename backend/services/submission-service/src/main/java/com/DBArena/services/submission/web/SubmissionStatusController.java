package com.DBArena.services.submission.web;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.submission.repository.SubmissionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * B03: service-to-service only - same {@code /internal/...} convention as
 * user-service's {@code BookmarkQueryController} (see its Javadoc for the
 * full rationale). Called by catalog-service's {@code SubmissionServiceClient}
 * to resolve the {@code status} filter on {@code /api/v1/problems}.
 */
@RestController
public class SubmissionStatusController {

    private final SubmissionRepository submissionRepository;

    public SubmissionStatusController(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/internal/v1/users/{userId}/problem-statuses")
    public Map<String, String> problemStatuses(@PathVariable String userId) {
        return submissionRepository.findStatusesByUserId(TypedId.<AuthenticatedUser>of(userId));
    }
}
