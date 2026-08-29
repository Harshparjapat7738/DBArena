package com.DBArena.services.catalog.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Deliberately the plain Mongo sync driver, not {@code spring-boot-starter-data-mongodb}.
 * This platform's convention (see identity-service/B14) is to keep
 * persistence code close to plain records and driver calls rather than an
 * ORM/ODM abstraction - Spring Data MongoDB's repository proxies and
 * {@code @Document} entity-mapping machinery would be exactly the kind of
 * framework root CLAUDE.md's "do not introduce ... without asking" line is
 * about. {@link com.DBArena.services.catalog.repository.MongoProblemRepository}
 * talks to {@link MongoCollection}&lt;{@link Document}&gt; directly.
 */
@Configuration
@EnableConfigurationProperties(CatalogProperties.class)
public class MongoConfig {

    public static final String PROBLEMS_COLLECTION = "problems";
    public static final String TOPICS_COLLECTION = "topics";
    public static final String DATASET_METADATA_COLLECTION = "dataset_metadata";
    public static final String LEARNING_PATHS_COLLECTION = "learning_paths";

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(CatalogProperties properties) {
        return MongoClients.create(properties.getMongoUri());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient, CatalogProperties properties) {
        return mongoClient.getDatabase(properties.getMongoDatabase());
    }

    @Bean
    public MongoCollection<Document> problemsCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(PROBLEMS_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> topicsCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(TOPICS_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> datasetMetadataCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(DATASET_METADATA_COLLECTION);
    }

    @Bean
    public MongoCollection<Document> learningPathsCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(LEARNING_PATHS_COLLECTION);
    }
}
