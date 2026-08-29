package com.DBArena.services.gamification.repository.progress;

import com.DBArena.common.core.id.TypedId;
import com.DBArena.common.security.AuthenticatedUser;
import com.DBArena.services.gamification.domain.progress.SkillMastery;
import com.DBArena.services.gamification.domain.progress.UserProgress;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserProgressDocumentMapperTest {

    @Test
    void roundTripsEveryFieldIncludingEmbeddedMasteries() {
        UserProgress progress = new UserProgress(
                TypedId.<AuthenticatedUser>of("01J000USER"),
                1250L,
                5,
                250L,
                500L,
                7,
                12,
                Optional.of("2026-08-29"),
                2,
                List.of(new SkillMastery("arrays", 80, 8, 10)),
                1_700_000_000_000L);

        Document document = UserProgressDocumentMapper.toDocument(progress);
        UserProgress roundTripped = UserProgressDocumentMapper.fromDocument(document);

        assertThat(roundTripped).isEqualTo(progress);
        assertThat(document.get("_id")).isEqualTo("01J000USER"); // keyed on userId, not a generated id
    }

    @Test
    void initialProgressStartsAtLevelOneWithNoStreak() {
        UserProgress progress = UserProgress.initial(TypedId.of("01J000USER"), 1000L);

        assertThat(progress.xp()).isZero();
        assertThat(progress.level()).isEqualTo(1);
        assertThat(progress.streakLastActiveDate()).isEmpty();
    }
}
