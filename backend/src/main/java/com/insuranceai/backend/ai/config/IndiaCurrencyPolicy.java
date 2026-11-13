package com.insuranceai.backend.ai.config;

/** Shared INR contract for every AI system prompt. */
public final class IndiaCurrencyPolicy {
    private IndiaCurrencyPolicy() {}

    public static final String INSTRUCTION = """
            Currency contract: all platform monetary amounts are Indian rupees (INR), including
            coverageAmount, premiumAmount, claimAmount, approvedAmount and revisedPremiumAmount.
            Display platform amounts as INR or \u20B9, never as dollars or USD. Preserve the numeric
            value exactly; do not perform FX conversion or multiply amounts. Tool arguments must
            remain numeric, without currency symbols or grouping separators.
            If a user requests a platform amount in another currency, ask for an INR amount.
            For knowledge sources explicitly denominated in another currency, retain the source's
            currency attribution; do not relabel it as INR or convert it. Explain the difference
            from the platform's INR contract and never invent an exchange rate.
            """;

    public static String systemPrompt(String domainPrompt) {
        return domainPrompt + "\n" + INSTRUCTION;
    }
}
