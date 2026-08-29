package com.DBArena.services.submission.config;

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
@EnableConfigurationProperties(SubmissionProperties.class)
public class MongoConfig {

    public static final String SUBMISSIONS_COLLECTION = "submissions";

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(SubmissionProperties properties) {
        return MongoClients.create(properties.getMongoUri());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient, SubmissionProperties properties) {
        return mongoClient.getDatabase(properties.getMongoDatabase());
    }

    @Bean
    public MongoCollection<Document> submissionsCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(SUBMISSIONS_COLLECTION);
    }
}
