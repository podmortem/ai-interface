package com.redhat.podmortem.ai.rest;

import com.redhat.podmortem.ai.service.AnalysisService;
import com.redhat.podmortem.common.model.analysis.AnalysisRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.jboss.logging.Logger;

@Path("/api/v1/ai-analysis")
@ApplicationScoped
public class Analysis {

    private static final Logger LOG = Logger.getLogger(Analysis.class);

    @Inject AnalysisService aiAnalysisService;

    @POST
    @Path("/explain")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> explainFailure(AnalysisRequest request) {
        LOG.infof(
                "Received AI analysis request for analysis ID: %s",
                request.getAnalysisResult().getAnalysisId());

        return aiAnalysisService
                .analyzeFailure(request.getAnalysisResult(), request.getProviderConfig())
                .map(
                        response -> {
                            LOG.infof(
                                    "AI analysis completed for analysis ID: %s",
                                    request.getAnalysisResult().getAnalysisId());
                            return Response.ok(response).build();
                        })
                .onFailure()
                .recoverWithItem(
                        throwable -> {
                            LOG.errorf(
                                    throwable,
                                    "AI analysis failed for analysis ID: %s",
                                    request.getAnalysisResult().getAnalysisId());
                            return Response.status(500)
                                    .entity(
                                            Map.of(
                                                    "error", "AI analysis failed",
                                                    "message", throwable.getMessage(),
                                                    "analysisId",
                                                            request.getAnalysisResult()
                                                                    .getAnalysisId()))
                                    .build();
                        });
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        return Response.ok(
                        Map.of(
                                "status", "UP",
                                "service", "ai-interface",
                                "timestamp", System.currentTimeMillis()))
                .build();
    }

    @GET
    @Path("/providers")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> listProviders() {
        return aiAnalysisService
                .getAvailableProviders()
                .map(providers -> Response.ok(Map.of("providers", providers)).build())
                .onFailure()
                .recoverWithItem(
                        throwable ->
                                Response.status(500)
                                        .entity(
                                                Map.of(
                                                        "error",
                                                        "Failed to list providers",
                                                        "message",
                                                        throwable.getMessage()))
                                        .build());
    }
}
