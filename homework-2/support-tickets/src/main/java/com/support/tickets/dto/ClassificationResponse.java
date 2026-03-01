package com.support.tickets.dto;

import com.support.tickets.model.TicketCategory;
import com.support.tickets.model.TicketPriority;

import java.util.List;
import java.util.UUID;

public class ClassificationResponse {
    private UUID ticketId;
    private TicketCategory category;
    private TicketPriority priority;
    private double confidence;
    private String reasoning;
    private List<String> keywordsFound;

    public ClassificationResponse() {}

    public ClassificationResponse(UUID ticketId, TicketCategory category, TicketPriority priority,
                                   double confidence, String reasoning, List<String> keywordsFound) {
        this.ticketId = ticketId;
        this.category = category;
        this.priority = priority;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.keywordsFound = keywordsFound;
    }

    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }

    public TicketCategory getCategory() { return category; }
    public TicketCategory getSuggestedCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public List<String> getKeywordsFound() { return keywordsFound; }
    public void setKeywordsFound(List<String> keywordsFound) { this.keywordsFound = keywordsFound; }
}
