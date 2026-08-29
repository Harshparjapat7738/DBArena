package com.DBArena.services.execution.repository;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.execution.domain.Execution;

import java.util.Optional;

public interface ExecutionRepository {

    void insert(Execution execution);

    /** Full-document replace, keyed on id - every status transition is a replace, per {@code Execution}'s own immutable-record design. */
    void replace(Execution execution);

    Optional<Execution> findById(TypedId<Execution> id);

    /** Most-recent-first, per user - also the ownership check's data source (a caller only ever looks up their own executions, enforced by {@code ExecutionService}). */
    CursorPage<Execution> findPageByUserId(TypedId<AuthenticatedUser> userId, PageRequest pageRequest);
}
