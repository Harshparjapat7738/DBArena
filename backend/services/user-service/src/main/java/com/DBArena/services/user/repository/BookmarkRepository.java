package com.DBArena.services.user.repository;

import com.DBArena.common.core.pagination.CursorPage;
import com.DBArena.common.core.pagination.PageRequest;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.common.core.id.TypedId;
import com.DBArena.services.user.domain.Bookmark;

import java.util.List;

public interface BookmarkRepository {

    void insert(Bookmark bookmark);

    /** No-op if absent - toggling off an already-absent bookmark is not an error. */
    void deleteByUserIdAndProblemSlug(TypedId<AuthenticatedUser> userId, String problemSlug);

    boolean existsByUserIdAndProblemSlug(TypedId<AuthenticatedUser> userId, String problemSlug);

    CursorPage<Bookmark> findPageByUserId(TypedId<AuthenticatedUser> userId, PageRequest pageRequest);

    /**
     * B03: every problemSlug a user has bookmarked, unpaginated - backs
     * catalog-service's {@code bookmarked} filter (see
     * {@code BookmarkQueryController}). Bounded by how many problems exist
     * to bookmark in the first place, not by submission volume, so a full
     * in-memory list is fine here in a way it would not be for
     * submission-service's collection.
     */
    List<String> findAllProblemSlugsByUserId(TypedId<AuthenticatedUser> userId);
}
