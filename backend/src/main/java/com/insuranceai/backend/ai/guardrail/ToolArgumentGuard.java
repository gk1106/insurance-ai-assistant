package com.insuranceai.backend.ai.guardrail;

import com.insuranceai.backend.common.exception.BusinessRuleViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Shared argument validation for every tool the LLM can call (chat-agent tools and MCP tools
 * alike) -- the tool/action guardrail layer's "validate all tool arguments" requirement in one
 * place, rather than each tool class re-implementing its own UUID parsing and paging defaults.
 * Every failure here is a {@link BusinessRuleViolationException} (400, clean message), never a
 * raw {@link IllegalArgumentException} whose message exposes a Java type name to the caller.
 */
public final class ToolArgumentGuard {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private ToolArgumentGuard() {
    }

    /** Parses a required UUID-shaped tool argument, or throws a clean, named error. */
    public static UUID requireUuid(String value, String argumentName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(argumentName + " is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(
                    argumentName + " must be a valid id (UUID), got: '" + value + "'");
        }
    }

    /** Same as {@link #requireUuid}, but blank/null is a valid "not provided" rather than an error. */
    public static Optional<UUID> optionalUuid(String value, String argumentName) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(requireUuid(value, argumentName));
    }

    /**
     * Builds a {@link Pageable} from tool-supplied page/size arguments, clamping size to a sane
     * upper bound rather than trusting whatever the model (or a malicious MCP client) sends --
     * page/size were previously unbounded, letting a single call request an arbitrarily large
     * page.
     */
    public static Pageable pageable(Integer page, Integer size) {
        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveSize = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        return PageRequest.of(effectivePage, effectiveSize);
    }
}
