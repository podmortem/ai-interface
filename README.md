# ai-interface

A Quarkus-based REST API service providing AI pod failure analysis for the Podmortem operator.

## Overview

This service acts as the primary interface for AI analysis operations, orchestrating interactions between the log parser results and various AI providers. It provides fault-tolerant endpoints for generating human-readable explanations of Kubernetes pod failures.

## REST Endpoints

- `POST /api/v1/analysis/analyze` - Analyze pod failures using AI providers
- `POST /api/v1/analysis/validate` - Validate AI provider configurations
- `GET /api/v1/analysis/providers` - List available AI providers
- `POST /api/v1/analysis/prompts/reload` - Reload prompt templates
- `GET /api/v1/analysis/prompts/status` - Get prompt template status

## Dependencies

- `common-lib` - Shared models and interfaces
- `ai-provider-lib` - AI provider implementations

## Building

```bash
./mvnw package
```

For native compilation:
```bash
./mvnw package -Dnative
```
