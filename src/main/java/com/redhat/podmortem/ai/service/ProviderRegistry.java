package com.redhat.podmortem.ai.service;

import com.redhat.podmortem.common.model.provider.AIProvider;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Registry for managing AI provider instances and their availability.
 *
 * <p>Automatically discovers and registers all available AI provider implementations at startup
 * using CDI. Provides thread-safe access to AI providers by ID and maintains a registry of all
 * available providers for the analysis service.
 */
@ApplicationScoped
public class ProviderRegistry {

    private static final Logger LOG = Logger.getLogger(ProviderRegistry.class);

    @Inject Instance<AIProvider> providerInstances;

    private Map<String, AIProvider> providers;

    /**
     * Initializes the provider registry during application startup.
     *
     * <p>Discovers all AI provider implementations using CDI and registers them by their provider
     * ID. This method is called automatically after dependency injection is complete.
     */
    @PostConstruct
    void initializeProviders() {
        this.providers = new ConcurrentHashMap<>();

        for (AIProvider provider : providerInstances) {
            providers.put(provider.getProviderId(), provider);
            LOG.infof("Registered AI provider: %s", provider.getProviderId());
        }

        LOG.infof("AI Provider Registry initialized with %d providers", providers.size());
    }

    /**
     * Retrieves an AI provider by its unique identifier.
     *
     * @param providerId the unique identifier of the AI provider
     * @return the AI provider instance
     * @throws IllegalArgumentException if the provider ID is not registered
     */
    public AIProvider getProvider(String providerId) {
        AIProvider provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException(
                    "Unknown AI provider: "
                            + providerId
                            + ". Available providers: "
                            + providers.keySet());
        }
        return provider;
    }

    /**
     * Gets all registered AI provider instances.
     *
     * @return a list of all available AI providers
     */
    public List<AIProvider> getAllProviders() {
        return new ArrayList<>(providers.values());
    }

    /**
     * Checks if a specific AI provider is available.
     *
     * @param providerId the provider ID to check
     * @return true if the provider is registered, false otherwise
     */
    public boolean isProviderAvailable(String providerId) {
        return providers.containsKey(providerId);
    }

    /**
     * Gets the list of all available provider IDs.
     *
     * @return a list of registered provider identifiers
     */
    public List<String> getAvailableProviderIds() {
        return new ArrayList<>(providers.keySet());
    }
}
