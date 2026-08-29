package com.DBArena.services.gamification.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Plain Mongo sync driver, not Spring Data MongoDB - see catalog-service's MongoConfig Javadoc for why. */
@Configuration
@EnableConfigurationProperties(GamificationProperties.class)
public class MongoConfig {

    public static final String USER_PROGRESS_COLLECTION = "user_progress";
    public static final String ACTIVITY_COLLECTION = "activity_log";
    public static final String BADGE_DEFINITIONS_COLLECTION = "badge_definitions";
    public static final String USER_BADGES_COLLECTION = "user_badges";
    public static final String DAILY_CHALLENGES_COLLECTION = "daily_challenges";
    public static final String DAILY_CHALLENGE_COMPLETIONS_COLLECTION = "daily_challenge_completions";
    public static final String LEADERBOARD_ENTRIES_COLLECTION = "leaderboard_entries";

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(GamificationProperties properties) {
        return MongoClients.create(properties.getMongoUri());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient, GamificationProperties properties) {
        return mongoClient.getDatabase(properties.getMongoDatabase());
    }

    @Bean
    public MongoCollection<Document> userProgressCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(USER_PROGRESS_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> activityCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(ACTIVITY_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> badgeDefinitionsCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(BADGE_DEFINITIONS_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> userBadgesCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(USER_BADGES_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> dailyChallengesCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(DAILY_CHALLENGES_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> dailyChallengeCompletionsCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(DAILY_CHALLENGE_COMPLETIONS_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> leaderboardEntriesCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(LEADERBOARD_ENTRIES_COLLECTION);
    }
}
