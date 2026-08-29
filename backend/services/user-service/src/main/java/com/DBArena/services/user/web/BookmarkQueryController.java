package com.DBArena.services.user.web;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.user.repository.BookmarkRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * B03: service-to-service only, called directly by catalog-service's
 * {@code UserServiceClient} (Feign, hard rule #2 - never a module
 * dependency), never through api-gateway - same "{@code /internal/...}, not
 * {@code /api/v1/...}" convention keeps it out of the gateway's route table
 * and public-path allowlist entirely, so it is unreachable from a browser
 * regardless of gateway config. No auth check of its own: the caller
 * (catalog-service) already knows which user it's asking about from *its
 * own* verified JWT, and this endpoint hands back nothing more sensitive
 * than a list of slugs for a userId the caller already possesses.
 */
@RestController
public class BookmarkQueryController {

    private final BookmarkRepository bookmarkRepository;

    public BookmarkQueryController(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    @GetMapping("/internal/v1/users/{userId}/bookmarked-slugs")
    public List<String> bookmarkedSlugs(@PathVariable String userId) {
        return bookmarkRepository.findAllProblemSlugsByUserId(TypedId.<AuthenticatedUser>of(userId));
    }
}
