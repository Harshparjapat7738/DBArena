package com.DBArena.services.gamification.mongock;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;

/** One changelog for every B02 gamification collection - all created together, all equally new. */
@ChangeUnit(id = "001-create-gamification-indexes", order = "001", author = "gamification-service")
public class ChangeLog001CreateGamificationIndexes {

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        // user_progress: _id IS the userId (see UserProgress's Javadoc) - no extra index needed for point lookups.

        var activity = mongoDatabase.getCollection("activity_log");
        activity.createIndex(Indexes.descending("userId", "occurredAt", "_id"));

        var badgeDefinitions = mongoDatabase.getCollection("badge_definitions");
        badgeDefinitions.createIndex(Indexes.ascending("slug"), new IndexOptions().unique(true));

        var userBadges = mongoDatabase.getCollection("user_badges");
        userBadges.createIndex(Indexes.ascending("userId", "badgeSlug"), new IndexOptions().unique(true));

        var dailyChallenges = mongoDatabase.getCollection("daily_challenges");
        dailyChallenges.createIndex(Indexes.ascending("date"), new IndexOptions().unique(true));

        var dailyChallengeCompletions = mongoDatabase.getCollection("daily_challenge_completions");
        dailyChallengeCompletions.createIndex(Indexes.ascending("userId", "date"), new IndexOptions().unique(true));

        var leaderboardEntries = mongoDatabase.getCollection("leaderboard_entries");
        leaderboardEntries.createIndex(Indexes.ascending("scope", "periodKey", "rank"));
        leaderboardEntries.createIndex(Indexes.ascending("scope", "periodKey", "userId"), new IndexOptions().unique(true));
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection("user_progress").drop();
        mongoDatabase.getCollection("activity_log").drop();
        mongoDatabase.getCollection("badge_definitions").drop();
        mongoDatabase.getCollection("user_badges").drop();
        mongoDatabase.getCollection("daily_challenges").drop();
        mongoDatabase.getCollection("daily_challenge_completions").drop();
        mongoDatabase.getCollection("leaderboard_entries").drop();
    }
}
