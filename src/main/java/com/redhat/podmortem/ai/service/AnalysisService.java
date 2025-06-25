package com.redhat.podmortem.ai.service;

import com.redhat.podmortem.common.model.analysis.AnalysisResult;
import com.redhat.podmortem.common.model.provider.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AnalysisService {

    private static final Logger LOG = Logger.getLogger(AnalysisService.class);

    @Inject ProviderRegistry providerRegistry;

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            successThreshold = 3,
            delay = 5000)
    @Retry(maxRetries = 3, delay = 1000)
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    public Uni<AIResponse> analyzeFailure(
            AnalysisResult analysisResult, AIProviderConfig providerConfig) {
        LOG.infof(
                "Starting AI analysis for analysis ID: %s using provider: %s",
                analysisResult.getAnalysisId(), providerConfig.getProviderId());

        try {
            // get the AI provider implementation from ai-provider-lib
            AIProvider provider = providerRegistry.getProvider(providerConfig.getProviderId());

            return provider.generateExplanation(analysisResult, providerConfig)
                    .map(response -> enrichResponse(response, analysisResult))
                    .onFailure()
                    .invoke(
                            throwable ->
                                    LOG.errorf(
                                            throwable,
                                            "AI provider call failed for provider: %s",
                                            providerConfig.getProviderId()));

        } catch (Exception e) {
            LOG.errorf(e, "Failed to get AI provider: %s", providerConfig.getProviderId());
            return Uni.createFrom().failure(e);
        }
    }

    @Fallback(fallbackMethod = "generateFallbackExplanation")
    public Uni<AIResponse> protectedAnalyzeFailure(
            AnalysisResult analysisResult, AIProviderConfig providerConfig) {
        return analyzeFailure(analysisResult, providerConfig);
    }

    public Uni<AIResponse> generateFallbackExplanation(
            AnalysisResult analysisResult, AIProviderConfig providerConfig) {
        LOG.warnf("Using fallback explanation for analysis ID: %s", analysisResult.getAnalysisId());

        // basic explanation based on analysis results when AI is unavailable
        String fallbackExplanation = buildBasicExplanation(analysisResult);

        AIResponse fallbackResponse = new AIResponse();
        fallbackResponse.setExplanation(fallbackExplanation);
        fallbackResponse.setProviderId("fallback");
        fallbackResponse.setModelId("pattern-based");
        fallbackResponse.setGeneratedAt(Instant.now());
        fallbackResponse.setProcessingTime(Duration.ofMillis(100));
        fallbackResponse.setConfidence(0.6); // Lower confidence for fallback

        return Uni.createFrom().item(fallbackResponse);
    }

    public Uni<List<String>> getAvailableProviders() {
        return Uni.createFrom()
                .item(
                        providerRegistry.getAllProviders().stream()
                                .map(AIProvider::getProviderId)
                                .collect(Collectors.toList()));
    }

    public Uni<ValidationResult> validateProvider(AIProviderConfig config) {
        try {
            AIProvider provider = providerRegistry.getProvider(config.getProviderId());
            return provider.validateConfiguration(config);
        } catch (Exception e) {
            ValidationResult result = new ValidationResult();
            result.setValid(false);
            result.setProviderId(config.getProviderId());
            result.setMessage("Provider not found: " + e.getMessage());
            return Uni.createFrom().item(result);
        }
    }

    private AIResponse enrichResponse(AIResponse response, AnalysisResult analysisResult) {
        // add any additional metadata or processing
        response.setGeneratedAt(Instant.now());

        // add correlation with analysis metadata
        if (response.getMetadata() == null) {
            response.setMetadata(
                    java.util.Map.of(
                            "analysisId",
                            analysisResult.getAnalysisId(),
                            "eventCount",
                            analysisResult.getEvents() != null
                                    ? analysisResult.getEvents().size()
                                    : 0));
        } else {
            response.getMetadata().put("analysisId", analysisResult.getAnalysisId());
            response.getMetadata()
                    .put(
                            "eventCount",
                            analysisResult.getEvents() != null
                                    ? analysisResult.getEvents().size()
                                    : 0);
        }

        return response;
    }

    private String buildBasicExplanation(AnalysisResult analysisResult) {
        StringBuilder explanation = new StringBuilder();

        explanation.append("Pod failure analysis (pattern-based fallback): ");

        if (analysisResult.getEvents() != null && !analysisResult.getEvents().isEmpty()) {
            // Get the first critical event
            var firstEvent = analysisResult.getEvents().get(0);

            // Access pattern ID and severity through the matched pattern
            if (firstEvent.getMatchedPattern() != null) {
                String patternId = firstEvent.getMatchedPattern().getId();
                String severity = firstEvent.getMatchedPattern().getSeverity();

                explanation
                        .append("The pod appears to have failed due to pattern '")
                        .append(patternId != null ? patternId : "unknown")
                        .append("' with severity ")
                        .append(severity != null ? severity : "unknown")
                        .append(". ");
            } else {
                explanation
                        .append("The pod appears to have failed with score ")
                        .append(firstEvent.getScore())
                        .append(" at line ")
                        .append(firstEvent.getLineNumber())
                        .append(". ");
            }

            if (analysisResult.getEvents().size() > 1) {
                explanation
                        .append("Additional ")
                        .append(analysisResult.getEvents().size() - 1)
                        .append(" event(s) were also detected.");
            }
        } else {
            explanation.append("No specific failure patterns were detected in the log analysis.");
        }

        return explanation.toString();
    }
}
