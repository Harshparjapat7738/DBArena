package com.dbforge.services.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code dbforge.ai.*}. Model ids default to the current production/GA
 * models documented for each provider as of this milestone (2026-08):
 * Groq's {@code llama-3.3-70b-versatile} (their strongest general-purpose
 * production model - see console.groq.com/docs/models) and Gemini's
 * {@code gemini-3-flash-preview} (Pro-level intelligence at Flash
 * speed/pricing - see ai.google.dev/gemini-api/docs/gemini-3). Both are
 * overridable per-environment; neither API key has a default - a blank
 * key is treated as "provider not configured" (see
 * {@code AiProviderConfig}) rather than sent to the provider.
 *
 * <p>{@code maxOutputTokens} is passed to the provider as a generation
 * hint. It is deliberately NOT the only enforcement of "output must be
 * compact" - {@link com.dbforge.services.ai.guard.OutputGuard} hard-caps
 * the response server-side regardless of what a provider actually
 * returns, the same "don't trust the request, verify the response"
 * posture hard rule #5 takes with the AI context builder's row cap.
 */
@ConfigurationProperties(prefix = "dbforge.ai")
public class AiProviderProperties {

    private Groq groq = new Groq();
    private Gemini gemini = new Gemini();
    private int maxOutputTokens = 220;
    private String datasetsRoot = "../datasets";

    public Groq getGroq() {
        return groq;
    }

    public void setGroq(Groq groq) {
        this.groq = groq;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public void setGemini(Gemini gemini) {
        this.gemini = gemini;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public String getDatasetsRoot() {
        return datasetsRoot;
    }

    public void setDatasetsRoot(String datasetsRoot) {
        this.datasetsRoot = datasetsRoot;
    }

    public static class Groq {
        private String apiKey = "";
        private String baseUrl = "https://api.groq.com/openai/v1/chat/completions";
        private String model = "llama-3.3-70b-versatile";
        private int timeoutMs = 8000;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Gemini {
        private String apiKey = "";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models";
        private String model = "gemini-3-flash-preview";
        private int timeoutMs = 8000;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
