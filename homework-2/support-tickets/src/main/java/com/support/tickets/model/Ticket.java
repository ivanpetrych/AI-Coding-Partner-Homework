package com.support.tickets.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @NotBlank
    @Email
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @NotBlank
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @NotBlank
    @Size(min = 1, max = 200)
    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status = TicketStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "assigned_to")
    private String assignedTo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ticket_tags", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Embedded
    private TicketMetadata metadata;

    @Column(name = "classification_confidence")
    private Double classificationConfidence;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }

    public TicketMetadata getMetadata() { return metadata; }
    public void setMetadata(TicketMetadata metadata) { this.metadata = metadata; }

    public Double getClassificationConfidence() { return classificationConfidence; }
    public void setClassificationConfidence(Double classificationConfidence) {
        this.classificationConfidence = classificationConfidence;
    }
}
