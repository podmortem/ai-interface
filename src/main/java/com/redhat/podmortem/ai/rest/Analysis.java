package com.redhat.podmortem.ai.rest;

import com.redhat.podmortem.ai.service.AnalysisService;
import com.redhat.podmortem.common.model.analysis.AnalysisRequest;
import com.redhat.podmortem.common.model.provider.AIProviderConfig;
import com.redhat.podmortem.common.model.provider.AIResponse;
import com.redhat.podmortem.provider.service.PromptTemplateService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/v1/analysis")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Analysis {

    private static final Logger log = LoggerFactory.getLogger(Analysis.class);

    @Inject AnalysisService analysisService;

    @Inject PromptTemplateService promptTemplateService;

    @POST
    @Path("/analyze")
    public Uni<Response> analyze(AnalysisRequest request) {
        log.info(
                "Received analysis request for provider: {}",
                request.getProviderConfig().getProviderId());

        return analysisService
                .analyzeFailure(request.getAnalysisResult(), request.getProviderConfig())
                .map(
                        response -> {
                            log.info(
                                    "Analysis completed successfully for provider: {}",
                                    request.getProviderConfig().getProviderId());
                            return Response.ok(response).build();
                        })
                .onFailure()
                .recoverWithItem(
                        throwable -> {
                            log.error("Analysis failed", throwable);
                            AIResponse errorResponse = new AIResponse();
                            errorResponse.setExplanation(
                                    "Analysis failed: " + throwable.getMessage());
                            errorResponse.setProviderId("error");
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                    .entity(errorResponse)
                                    .build();
                        });
    }

    @POST
    @Path("/validate")
    public Uni<Response> validateProvider(AIProviderConfig config) {
        log.info("Validating provider configuration: {}", config.getProviderId());

        return analysisService
                .validateProvider(config)
                .map(
                        result -> {
                            if (result.isValid()) {
                                return Response.ok(result).build();
                            } else {
                                return Response.status(Response.Status.BAD_REQUEST)
                                        .entity(result)
                                        .build();
                            }
                        });
    }

    @GET
    @Path("/providers")
    public Uni<Response> getAvailableProviders() {
        return analysisService
                .getAvailableProviders()
                .map(providers -> Response.ok(providers).build());
    }

    @POST
    @Path("/prompts/reload")
    public Response reloadPrompts() {
        log.info("Prompt reload requested - feature requires external prompt ConfigMap");
        return Response.ok(
                        Map.of(
                                "status", "info",
                                "message",
                                        "To reload prompts, delete and recreate the prompt ConfigMap"))
                .build();
    }

    @GET
    @Path("/prompts/status")
    public Response getPromptStatus() {
        return Response.ok(
                        Map.of(
                                "status", "info",
                                "message",
                                        "Prompt status available in ai-provider-lib service logs"))
                .build();
    }
}
