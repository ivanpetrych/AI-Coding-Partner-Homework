package com.support.tickets.dto;

import com.support.tickets.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TicketResponse {

    private UUID id;
    private String customerId;
    private String customerEmail;
    private String customerName;
    private String subject;
    private String description;
    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String assignedTo;
    private List<String> tags;
    private MetadataResponse metadata;
    private Double classificationConfidence;

    public static TicketResponse from(Ticket ticket) {
        TicketResponse r = new TicketResponse();
        r.id = ticket.getId();
        r.customerId = ticket.getCustomerId();
        r.customerEmail = ticket.getCustomerEmail();
        r.customerName = ticket.getCustomerName();
        r.subject = ticket.getSubject();
        r.description = ticket.getDescription();
        r.category = ticket.getCategory();
        r.priority = ticket.getPriority();
        r.status = ticket.getStatus();
        r.createdAt = ticket.getCreatedAt();
        r.updatedAt = ticket.getUpdatedAt();
        r.resolvedAt = ticket.getResolvedAt();
        r.assignedTo = ticket.getAssignedTo();
        r.tags = ticket.getTags();
        r.classificationConfidence = ticket.getClassificationConfidence();
        if (ticket.getMetadata() != null) {
            MetadataResponse meta = new MetadataResponse();
            meta.setSource(ticket.getMetadata().getSource());
            meta.setBrowser(ticket.getMetadata().getBrowser());
            meta.setDeviceType(ticket.getMetadata().getDeviceType());
            r.metadata = meta;
        }
        return r;
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketCategory getCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public MetadataResponse getMetadata() { return metadata; }
    public void setMetadata(MetadataResponse metadata) { this.metadata = metadata; }

    public Double getClassificationConfidence() { return classificationConfidence; }
    public void setClassificationConfidence(Double classificationConfidence) {
        this.classificationConfidence = classificationConfidence;
    }
}
