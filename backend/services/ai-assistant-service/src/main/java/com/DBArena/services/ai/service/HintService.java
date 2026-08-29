package com.DBArena.services.ai.service;

import com.DBArena.engine.spi.cdm.CdmDataset;
import com.DBArena.services.ai.client.CatalogProblemResponse;
import com.DBArena.services.ai.client.CatalogServiceClient;
import com.DBArena.services.ai.config.AiProviderProperties;
import com.DBArena.services.ai.context.AiContextBuilder;
import com.DBArena.services.ai.context.HintContext;
import com.DBArena.services.ai.dataset.DatasetContextLoader;
import com.DBArena.services.ai.domain.HintCommand;
import com.DBArena.services.ai.domain.HintResult;
import com.DBArena.services.ai.domain.ProblemNotFoundException;
import com.DBArena.services.ai.guard.OutputGuard;
import com.DBArena.services.ai.prompt.HintPromptBuilder;
import com.DBArena.services.ai.provider.AiCompletionRequest;
import com.DBArena.services.ai.provider.AiCompletionResult;
import com.DBArena.services.ai.provider.FallbackAiCompletionGateway;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates one hint request end to end: catalog lookup (Feign) -&gt;
 * dataset schema lookup (dataset-cli) -&gt; {@link AiContextBuilder} -&gt;
 * {@link HintPromptBuilder} -&gt; {@link FallbackAiCompletionGateway} -&gt;
 * {@link OutputGuard}. No business logic lives anywhere else - each
 * collaborator does exactly one step, this class only sequences them.
 */
@Service
public class HintService {

    private final CatalogServiceClient catalogServiceClient;
    private final DatasetContextLoader datasetContextLoader;
    private final AiContextBuilder contextBuilder;
    private final HintPromptBuilder promptBuilder;
    private final FallbackAiCompletionGateway completionGateway;
    private final OutputGuard outputGuard;
    private final int maxOutputTokens;

    public HintService(
            CatalogServiceClient catalogServiceClient,
            DatasetContextLoader datasetContextLoader,
            AiContextBuilder contextBuilder,
            HintPromptBuilder promptBuilder,
            FallbackAiCompletionGateway completionGateway,
            OutputGuard outputGuard,
            AiProviderProperties properties) {
        this.catalogServiceClient = catalogServiceClient;
        this.datasetContextLoader = datasetContextLoader;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.completionGateway = completionGateway;
        this.outputGuard = outputGuard;
        this.maxOutputTokens = properties.getMaxOutputTokens();
    }

    public HintResult generateHint(HintCommand command) {
        CatalogProblemResponse problem = fetchProblem(command.problemSlug());
        Optional<CdmDataset> dataset = datasetContextLoader.load(problem.datasetSlug());
        HintContext context = contextBuilder.build(problem, dataset, command);

        String systemPrompt = promptBuilder.buildSystemPrompt(command.level());
        String userPrompt = promptBuilder.buildUserPrompt(context);
        AiCompletionResult completion = completionGateway.complete(
                new AiCompletionRequest(systemPrompt, userPrompt, maxOutputTokens));

        OutputGuard.GuardedHint guarded = outputGuard.apply(completion);
        return new HintResult(command.problemSlug(), command.level(), guarded.text(), guarded.provider(),
                guarded.truncated());
    }

    private CatalogProblemResponse fetchProblem(String slug) {
        try {
            return catalogServiceClient.getProblem(slug);
        } catch (FeignException.NotFound e) {
            throw new ProblemNotFoundException(slug);
        }
    }
}
