package com.DBArena.services.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dbarena.catalog")
public class CatalogProperties {

    private String mongoUri = "mongodb://localhost:27017";
    private String mongoDatabase = "DBArena_catalog";
    private String userServiceUri = "http://localhost:8085";
    private String submissionServiceUri = "http://localhost:8087";

    public String getMongoUri() {
        return mongoUri;
    }

    public void setMongoUri(String mongoUri) {
        this.mongoUri = mongoUri;
    }

    public String getMongoDatabase() {
        return mongoDatabase;
    }

    public void setMongoDatabase(String mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public String getUserServiceUri() {
        return userServiceUri;
    }

    public void setUserServiceUri(String userServiceUri) {
        this.userServiceUri = userServiceUri;
    }

    public String getSubmissionServiceUri() {
        return submissionServiceUri;
    }

    public void setSubmissionServiceUri(String submissionServiceUri) {
        this.submissionServiceUri = submissionServiceUri;
    }
}
