package com.DBArena.services.identity.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Deliberately the plain Mongo sync driver, not {@code spring-boot-starter-data-mongodb}
 * - same "no ORM, stay close to records" posture catalog-service's own
 * {@code MongoConfig} established (see that class's Javadoc). identity-service
 * was Postgres/plain-JDBC through M14; this store swap is a later,
 * separately-decided change (see backend/CLAUDE.md's Session Log) - the
 * public {@link com.DBArena.services.identity.repository.UserRepository}/
 * {@link com.DBArena.services.identity.repository.RefreshTokenRepository}
 * interfaces AuthService depends on didn't change, only the implementation.
 */
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class MongoConfig {

    public static final String USERS_COLLECTION = "users";
    public static final String REFRESH_TOKENS_COLLECTION = "refresh_tokens";

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(IdentityProperties properties) {
        return MongoClients.create(properties.getMongoUri());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient, IdentityProperties properties) {
        return mongoClient.getDatabase(properties.getMongoDatabase());
    }

    @Bean
    public MongoCollection<Document> usersCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(USERS_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> refreshTokensCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(REFRESH_TOKENS_COLLECTION);
    }
}
