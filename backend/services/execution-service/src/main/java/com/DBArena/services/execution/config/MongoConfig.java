package com.DBArena.services.execution.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Plain Mongo sync driver, not Spring Data MongoDB - see catalog-service's MongoConfig Javadoc for why. */
@Configuration
public class MongoConfig {

    public static final String EXECUTIONS_COLLECTION = "executions";

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(ExecutionProperties properties) {
        return MongoClients.create(properties.getMongoUri());
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient, ExecutionProperties properties) {
        return mongoClient.getDatabase(properties.getMongoDatabase());
    }

    @Bean
    public MongoCollection<Document> executionsCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(EXECUTIONS_COLLECTION);
    }
}
