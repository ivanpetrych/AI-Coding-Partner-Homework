package com.support.tickets.model;

import java.util.List;

public class ClassificationResult {

    private final TicketCategory category;
    private final TicketPriority priority;
    private final double confidence;
    private final String reasoning;
    private final List<String> keywordsFound;

    public ClassificationResult(TicketCategory category, TicketPriority priority,
                                double confidence, String reasoning, List<String> keywordsFound) {
        this.category = category;
        this.priority = priority;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.keywordsFound = keywordsFound;
    }

    public TicketCategory getCategory() { return category; }
    public TicketPriority getPriority() { return priority; }
    public double getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public List<String> getKeywordsFound() { return keywordsFound; }
}
