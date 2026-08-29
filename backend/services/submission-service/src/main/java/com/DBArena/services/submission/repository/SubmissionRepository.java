package com.DBArena.services.submission.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.submission.domain.Submission;

import java.util.Map;
import java.util.Optional;

public interface SubmissionRepository {

    void insert(Submission submission);

    /** Full-document replace, keyed on id - used once grading (B11, not this milestone) fills in a result. */
    void replace(Submission submission);

    Optional<Submission> findById(TypedId<Submission> id);

    /** Most-recent-first, per user. */
    CursorPage<Submission> findPageByUserId(TypedId<AuthenticatedUser> userId, PageRequest pageRequest);

    /** Most-recent-first, per (user, problem) - the workbench's "my past attempts on this problem" list. */
    CursorPage<Submission> findPageByUserIdAndProblemSlug(
            TypedId<AuthenticatedUser> userId, String problemSlug, PageRequest pageRequest);

    /**
     * B03: {@code problemSlug -> "SOLVED"|"ATTEMPTED"}, one entry per
     * problem the user has ever submitted against ({@code "SOLVED"} if any
     * of their submissions on it is {@code ACCEPTED}, {@code "ATTEMPTED"}
     * otherwise) - a slug absent from the map means "not started". Backs
     * catalog-service's {@code status} filter. Always empty today (nothing
     * populates this collection until B09-B11 exist) - a correct, real
     * aggregation over whatever data exists, not a stub.
     */
    Map<String, String> findStatusesByUserId(TypedId<AuthenticatedUser> userId);
}
