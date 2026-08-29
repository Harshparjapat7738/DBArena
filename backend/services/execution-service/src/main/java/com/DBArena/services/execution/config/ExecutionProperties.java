package com.DBArena.services.execution.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dbarena.execution")
public class ExecutionProperties {

    private String mongoUri = "mongodb://localhost:27017";
    private String mongoDatabase = "DBArena_execution";
    private String datasetsRoot = "../../../datasets";
    private Postgres postgres = new Postgres();
    private Policy policy = new Policy();

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

    public String getDatasetsRoot() {
        return datasetsRoot;
    }

    public void setDatasetsRoot(String datasetsRoot) {
        this.datasetsRoot = datasetsRoot;
    }

    public Postgres getPostgres() {
        return postgres;
    }

    public void setPostgres(Postgres postgres) {
        this.postgres = postgres;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    /** The sandbox role's own connection details - see SandboxProvider's Javadoc. Never populated from an HTTP request. */
    public static class Postgres {
        private String host = "localhost";
        private int port = 5432;
        private String username = "dbarena_sandbox";
        private String password = "";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /** Safe-default policy (B04) - every field here is a server-side ceiling, never client-overridable. */
    public static class Policy {
        private int maxStatementLength = 5000;
        private int maxResultRows = 500;
        private long maxResultBytes = 1_048_576;
        private int statementTimeoutSeconds = 5;
        private int explainTimeoutSeconds = 3;
        private int maxConcurrentPerUser = 2;
        private int maxConcurrentGlobal = 8;
        private int sandboxConnectionLimit = 3;

        public int getMaxStatementLength() {
            return maxStatementLength;
        }

        public void setMaxStatementLength(int maxStatementLength) {
            this.maxStatementLength = maxStatementLength;
        }

        public int getMaxResultRows() {
            return maxResultRows;
        }

        public void setMaxResultRows(int maxResultRows) {
            this.maxResultRows = maxResultRows;
        }

        public long getMaxResultBytes() {
            return maxResultBytes;
        }

        public void setMaxResultBytes(long maxResultBytes) {
            this.maxResultBytes = maxResultBytes;
        }

        public int getStatementTimeoutSeconds() {
            return statementTimeoutSeconds;
        }

        public void setStatementTimeoutSeconds(int statementTimeoutSeconds) {
            this.statementTimeoutSeconds = statementTimeoutSeconds;
        }

        public int getExplainTimeoutSeconds() {
            return explainTimeoutSeconds;
        }

        public void setExplainTimeoutSeconds(int explainTimeoutSeconds) {
            this.explainTimeoutSeconds = explainTimeoutSeconds;
        }

        public int getMaxConcurrentPerUser() {
            return maxConcurrentPerUser;
        }

        public void setMaxConcurrentPerUser(int maxConcurrentPerUser) {
            this.maxConcurrentPerUser = maxConcurrentPerUser;
        }

        public int getMaxConcurrentGlobal() {
            return maxConcurrentGlobal;
        }

        public void setMaxConcurrentGlobal(int maxConcurrentGlobal) {
            this.maxConcurrentGlobal = maxConcurrentGlobal;
        }

        public int getSandboxConnectionLimit() {
            return sandboxConnectionLimit;
        }

        public void setSandboxConnectionLimit(int sandboxConnectionLimit) {
            this.sandboxConnectionLimit = sandboxConnectionLimit;
        }
    }
}
