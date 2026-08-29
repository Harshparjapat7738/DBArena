package com.DBArena.services.user.config;

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
@EnableConfigurationProperties(UserProperties.class)
public class MongoConfig {

    public static final String BOOKMARKS_COLLECTION = "bookmarks";

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(UserProperties properties) {
        return MongoClients.create(properties.getMongoUri());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient, UserProperties properties) {
        return mongoClient.getDatabase(properties.getMongoDatabase());
    }

    @Bean
    public MongoCollection<Document> bookmarksCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(BOOKMARKS_COLLECTION);
    }
}
