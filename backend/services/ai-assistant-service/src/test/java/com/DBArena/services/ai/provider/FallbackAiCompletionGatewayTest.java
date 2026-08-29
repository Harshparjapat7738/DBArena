package com.DBArena.services.ai.provider;

import com.DBArena.services.ai.domain.AiUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FallbackAiCompletionGatewayTest {

    private static final AiCompletionRequest REQUEST = new AiCompletionRequest("system", "user", 100);

    @Test
    void usesThePrimaryWhenItSucceeds() {
        GroqCompletionClient groq = mock(GroqCompletionClient.class);
        GeminiCompletionClient gemini = mock(GeminiCompletionClient.class);
        when(groq.name()).thenReturn("groq");
        when(groq.complete(REQUEST)).thenReturn(new AiCompletionResult("hint from groq", "groq"));

        AiCompletionResult result = new FallbackAiCompletionGateway(groq, gemini).complete(REQUEST);

        assertThat(result.providerName()).isEqualTo("groq");
        assertThat(result.text()).isEqualTo("hint from groq");
    }

    @Test
    void fallsBackToGeminiWhenGroqThrows() {
        GroqCompletionClient groq = mock(GroqCompletionClient.class);
        GeminiCompletionClient gemini = mock(GeminiCompletionClient.class);
        when(groq.name()).thenReturn("groq");
        when(gemini.name()).thenReturn("gemini");
        when(groq.complete(REQUEST)).thenThrow(new AiProviderException("groq", "HTTP 500"));
        when(gemini.complete(REQUEST)).thenReturn(new AiCompletionResult("hint from gemini", "gemini"));

        AiCompletionResult result = new FallbackAiCompletionGateway(groq, gemini).complete(REQUEST);

        assertThat(result.providerName()).isEqualTo("gemini");
        assertThat(result.text()).isEqualTo("hint from gemini");
    }

    @Test
    void throwsAiUnavailableWhenBothProvidersFail() {
        GroqCompletionClient groq = mock(GroqCompletionClient.class);
        GeminiCompletionClient gemini = mock(GeminiCompletionClient.class);
        when(groq.name()).thenReturn("groq");
        when(gemini.name()).thenReturn("gemini");
        when(groq.complete(REQUEST)).thenThrow(new AiProviderException("groq", "not configured"));
        when(gemini.complete(REQUEST)).thenThrow(new AiProviderException("gemini", "HTTP 503"));

        assertThatThrownBy(() -> new FallbackAiCompletionGateway(groq, gemini).complete(REQUEST))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void geminiIsNeverCalledWhenGroqSucceeds() {
        GroqCompletionClient groq = mock(GroqCompletionClient.class);
        GeminiCompletionClient gemini = mock(GeminiCompletionClient.class);
        when(groq.name()).thenReturn("groq");
        when(groq.complete(REQUEST)).thenReturn(new AiCompletionResult("ok", "groq"));

        new FallbackAiCompletionGateway(groq, gemini).complete(REQUEST);

        org.mockito.Mockito.verifyNoInteractions(gemini);
    }
}
