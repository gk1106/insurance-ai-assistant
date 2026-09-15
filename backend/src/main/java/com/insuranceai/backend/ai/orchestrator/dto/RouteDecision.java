package com.insuranceai.backend.ai.orchestrator.dto;

import java.util.List;

/**
 * Raw structured output from the router LLM call. {@code agents} is kept as free-form strings
 * (rather than an enum) because it comes straight from the model -- {@code AiOrchestratorService}
 * parses each entry leniently and drops anything it doesn't recognize, rather than letting a
 * malformed value fail the whole request.
 */
public record RouteDecision(List<String> agents, String reasoning) {
}
