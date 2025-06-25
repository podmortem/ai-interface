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

@ApplicationScoped
public class ProviderRegistry {

    private static final Logger LOG = Logger.getLogger(ProviderRegistry.class);

    @Inject Instance<AIProvider> providerInstances;

    private Map<String, AIProvider> providers;

    @PostConstruct
    void initializeProviders() {
        this.providers = new ConcurrentHashMap<>();

        for (AIProvider provider : providerInstances) {
            providers.put(provider.getProviderId(), provider);
            LOG.infof("Registered AI provider: %s", provider.getProviderId());
        }

        LOG.infof("AI Provider Registry initialized with %d providers", providers.size());
    }

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

    public List<AIProvider> getAllProviders() {
        return new ArrayList<>(providers.values());
    }

    public boolean isProviderAvailable(String providerId) {
        return providers.containsKey(providerId);
    }

    public List<String> getAvailableProviderIds() {
        return new ArrayList<>(providers.keySet());
    }
}
